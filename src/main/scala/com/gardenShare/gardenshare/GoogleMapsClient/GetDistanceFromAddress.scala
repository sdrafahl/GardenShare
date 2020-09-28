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

case class Distance(seconds: Float)

abstract class IsWithinRange {
  def isInRange(range: Distance, dist: Distance): Boolean
}

object IsWithinRange {
  def apply() = default
  implicit object default extends IsWithinRange {
    def isInRange(range: Distance, dist: Distance): Boolean = dist.seconds < range.seconds
  }
  implicit class Ops(underlying: Distance) {
    def inRange(range: Distance)(implicit isWith: IsWithinRange) = isWith.isInRange(range, underlying)
  }
}

abstract class GetDistance[F[_]] {
  def getDistanceFromAddress(from: Address, to: Address)(implicit getKey: GetGoogleMapsApiKey[F]): F[Distance]
}

object GetDistance {
  def apply[F[_]: GetDistance: GetGoogleMapsApiKey]() = implicitly[GetDistance[F]]
  implicit object IOGetDistance extends GetDistance[IO] {
    implicit val default = GetGoogleMapsApiKey[IO]()
    def getDistanceFromAddress(from: Address, to: Address)(implicit getKey: GetGoogleMapsApiKey[IO]): IO[Distance] = {
      val getContPgm = for {
        key <- getKey.get
        cont = new GeoApiContext.Builder().apiKey(key.key).build()
        now = Instant.now()
        dir = DirectionsApi.newRequest(cont).departureTimeNow().origin(from.underlying).destination(to.underlying).await()
        maybeFirstRoute = dir.routes.headOption
        distance = maybeFirstRoute.map {r =>
          r.legs.foldLeft(Duration.ZERO) {
            case (acc, leg) => {
              val secs = leg.duration.inSeconds
              val accsecs = acc.getSeconds()
              val totalTime = secs + accsecs
              Duration.ofSeconds(totalTime)
            }
          }
        }
      } yield distance
      getContPgm.flatMap {
        case Some(dur) => IO(Distance(dur.getSeconds()))
        case None => IO.raiseError(new Throwable("No direction information was returned"))
      }
    }
  }
}
