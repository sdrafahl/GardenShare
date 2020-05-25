package com.gardenShare.gardenshare

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.gardenShare.gardenshare.Storage.Relational.Setup

object Main extends IOApp {
  def run(args: List[String]) = for {
    _ <- Setup.createPostGresDBTables
    exitCode <- GardenshareServer.stream[IO].compile.drain.as(ExitCode.Success)
  } yield exitCode
}
