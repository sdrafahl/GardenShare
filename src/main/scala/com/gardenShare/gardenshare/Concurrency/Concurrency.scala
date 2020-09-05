package com.gardenShare.gardenshare.Concurrency

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import cats.effect.ContextShift

object Concurrency {
  implicit private val ec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  implicit val cs = IO.contextShift(ec)
}
