package com.gardenShare.gardenshare

import cats.effect.ConcurrentEffect
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.middleware._
import scala.concurrent.duration._
import com.gardenShare.gardenshare.GetEnvironment
import cats.effect.Timer

object GardenshareServer {

  def stream[F[_]:
      ConcurrentEffect:
      GetEnvironment:
      GetRoutesForEnv
  ](implicit T: Timer[F]): F[Unit] = {

    GetEnvironment().getEnv.flatMap { sysEnv =>
      val httpApp = (
          GetRoutesForEnv().getRoutesGivenEnv(sysEnv)
      ).orNotFound

      val finalHttpApp = Logger.httpApp(true, true)(httpApp)

      val methodConfig = CORSConfig(
          anyOrigin = true,
          anyMethod = true,
          allowCredentials = true,
        maxAge = 1.day.toSeconds)

      val corsService = CORS(finalHttpApp, methodConfig)

      (for {

        exitCode <- BlazeServerBuilder[F]
        .bindHttp(8055, "0.0.0.0")
        .withHttpApp(corsService)
        .enableHttp2(true)
        .serve
      } yield exitCode).drain.compile.drain
    }
  }
}
