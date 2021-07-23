package com.gardenShare.gardenshare

import com.google.maps.GeoApiContext
import com.google.maps.DirectionsApi
import com.gardenShare.gardenshare.GetGoogleMapsApiKey
import cats.Show
import cats.implicits._
import cats.effect.IO

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
  def getDistanceFromAddress(from: Address, to: Address): F[DistanceInMiles]
}

object GetDistance {
  def apply[F[_]: GetDistance: GetGoogleMapsApiKey]() = implicitly[GetDistance[F]]
  implicit def IOGetDistance(implicit s: Show[Address], getKey: GetGoogleMapsApiKey[IO]) = new GetDistance[IO] {
    def getDistanceFromAddress(from: Address, to: Address): IO[DistanceInMiles] = {
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
