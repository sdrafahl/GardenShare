package com.gardenShare.gardenshare

import cats.implicits._
import org.http4s.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.middleware._
import scala.concurrent.duration._
import com.gardenShare.gardenshare.GetEnvironment
import cats.effect.Temporal
import cats.effect.kernel.Async

object GardenshareServer {

  def stream[F[_]:
      GetEnvironment:
      GetRoutesForEnv:
      Async
  ](implicit T: Temporal[F]): F[Unit] = {

    GetEnvironment().getEnv.flatMap { sysEnv =>
      val httpApp = (
          GetRoutesForEnv().getRoutesGivenEnv(sysEnv)
      ).orNotFound

      val finalHttpApp = Logger.httpApp(true, true)(httpApp)

      (for {
        exitCode <- BlazeServerBuilder[F]
        .bindHttp(8055, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .enableHttp2(true)
        .serve
      } yield exitCode).drain.compile.drain
    }
  }
}
