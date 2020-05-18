package com.gardenShare.gardenshare

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  def run(args: List[String]) =
    GardenshareServer.stream[IO].compile.drain.as(ExitCode.Success)
}