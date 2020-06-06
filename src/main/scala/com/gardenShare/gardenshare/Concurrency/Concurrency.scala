package com.gardenShare.gardenshare.Concurrency

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO

object Concurrency {
  val ec = scala.concurrent.ExecutionContext.global
  implicit val cs = IO.contextShift(ec)
}
