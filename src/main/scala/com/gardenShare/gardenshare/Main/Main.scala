package com.gardenShare.gardenshare

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.gardenShare.gardenshare.PostGresSetup
import com.gardenShare.gardenshare.GetRoutesForEnv._
import com.gardenShare.gardenshare.GetRoutes._
import com.gardenShare.gardenshare.Shows._
import com.gardenShare.gardenshare.Encoders._
import io.circe.generic.auto._, io.circe.syntax._
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import com.gardenShare.gardenshare.GetTypeSafeConfig

object Main extends IOApp {
  val confPgm: IO[GetTypeSafeConfig[IO]] = IO(ConfigFactory.load()).map(conf => GetTypeSafeConfig.ioGetTypeSafeConfig(conf))

  def run(args: List[String]) = for {
    typeSafeConfig <- confPgm
    x <- {
      implicit val c = typeSafeConfig
      val concurrencyVals = ConcurrencyHelper.createConcurrencyValues(4)
      implicit val postgresClient = PostGresSetup.createPostgresClient
      GardenshareServer.stream[IO].as(ExitCode.Success)
    }
  } yield x
}
