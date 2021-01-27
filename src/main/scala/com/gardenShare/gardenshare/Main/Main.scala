package com.gardenShare.gardenshare

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.gardenShare.gardenshare.Storage.Relational.Setup
import com.gardenShare.gardenshare.GetRoutesForEnv._
import com.gardenShare.gardenshare.GetRoutes._
import com.gardenShare.gardenshare.Shows._
import com.gardenShare.gardenshare.Encoders.Encoders._

object Main extends IOApp {
  def run(args: List[String]) = for {
    exitCode <- GardenshareServer.stream[IO].as(ExitCode.Success)
  } yield exitCode
}
