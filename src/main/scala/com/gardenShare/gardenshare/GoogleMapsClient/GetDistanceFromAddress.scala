package com.gardenShare.gardenshare.GoogleMapsClient

import com.gardenShare.gardenshare.domain.Store._
import cats.effect.IO
import com.google.maps.GeoApiContext
import com.google.maps.DirectionsApi
import com.gardenShare.gardenshare.Config.GetGoogleMapsApiKey
import cats.effect.concurrent.Deferred
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import java.time._
import com.google.maps.model.DistanceMatrix
import java.time.Duration
import cats.instances.float
import cats.Show
import cats.implicits._
import com.google.maps.model.Distance

case class DistanceInMiles(distance: Double)

abstract class IsWithinRange {
  def isInRange(range: DistanceInMiles, dist: DistanceInMiles): Boolean
}

object IsWithinRange {
  def apply() = default
  implicit object default extends IsWithinRange {
    def isInRange(range: DistanceInMiles, dist: DistanceInMiles): Boolean = dist.distance < range.distance
  }
  implicit class Ops(underlying: DistanceInMiles) {
    def inRange(range: DistanceInMiles)(implicit isWith: IsWithinRange) = isWith.isInRange(range, underlying)
  }
}

abstract class GetDistance[F[_]] {
  def getDistanceFromAddress(from: Address, to: Address)(implicit getKey: GetGoogleMapsApiKey[F]): F[DistanceInMiles]
}

object GetDistance {
  def apply[F[_]: GetDistance: GetGoogleMapsApiKey]() = implicitly[GetDistance[F]]
  implicit def IOGetDistance(implicit s: Show[Address]) = new GetDistance[IO] {
    implicit val default = GetGoogleMapsApiKey[IO]()
    def getDistanceFromAddress(from: Address, to: Address)(implicit getKey: GetGoogleMapsApiKey[IO]): IO[DistanceInMiles] = {
      val maybeDistancepgm = for {
        key <- getKey.get
        cont = new GeoApiContext.Builder().apiKey(key.key).build()
        dir = DirectionsApi.newRequest(cont).departureTimeNow().origin(from.show).destination(to.show).await()
        maybeFirstRoute = dir.routes.headOption
        distance = maybeFirstRoute.map {r =>          
          r.legs.foldLeft(0.0) {
            case (acc, leg) => acc + leg.distance.inMeters
          }
        }
       distanceInMiles = distance.map(x => DistanceInMiles(x/1609.34))
      } yield distanceInMiles
      maybeDistancepgm.flatMap{
        case None => IO.raiseError(new Throwable("No Route found."))
        case Some(a) => IO(a)
      }
    }
  }
}
