package com.gardenShare.gardenshare

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.gardenShare.gardenshare.PostGresSetup
import com.gardenShare.gardenshare.GetRoutesForEnv._
import com.gardenShare.gardenshare.GetRoutes._
import io.circe.generic.auto._
import com.typesafe.config.ConfigFactory
import com.gardenShare.gardenshare.GetTypeSafeConfig
import ParsingDecodingImplicits._
import slick.jdbc.PostgresProfile

object Main extends IOApp {
  val confPgm: IO[GetTypeSafeConfig[IO]] = IO(ConfigFactory.load()).map(conf => GetTypeSafeConfig.ioGetTypeSafeConfig(conf))
  
  def run(args: List[String]) = for {    
    typeSafeConfig <- confPgm
    x <- {     
      implicit val c = typeSafeConfig
      val executionValues = ConcurrencyHelper.createConcurrencyValues(4)
      implicit val ec = executionValues._2      
      Resource.make(IO(PostGresSetup.createPostgresClient))((a: PostgresProfile.backend.DatabaseDef) => IO(a.close())).use{dbClient =>
        implicit val x = dbClient
        GardenshareServer.stream[IO].as(ExitCode.Success)
      }      
    }
  } yield x
}
