package com.gardenShare.gardenshare.Concurrency

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import cats.effect.ContextShift
import cats.effect.Blocker
import cats.effect.Concurrent
import cats.effect.Concurrent._

object Concurrency {
  implicit lazy val executor = Executors.newWorkStealingPool()
  implicit lazy val ec = ExecutionContext.fromExecutor(executor)
  implicit lazy val cs = IO.contextShift(ec)  
  implicit lazy val blocker = Blocker.liftExecutorService(executor)
  implicit lazy val timer = IO.timer(ec)
}
