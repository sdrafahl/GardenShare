package com.gardenShare.gardenshare

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import cats.effect.ContextShift
import cats.effect.Blocker
import cats.effect.Concurrent
import cats.effect.Concurrent._

object ConcurrencyHelper {
  def createConcurrencyValues(threadCount: Int) = {
    lazy val executor = Executors.newWorkStealingPool(threadCount)
    lazy val ec = ExecutionContext.fromExecutor(executor)
    lazy val cs = IO.contextShift(ec)
    lazy val blocker = Blocker.liftExecutorService(executor)
    lazy val timer = IO.timer(ec)
    (executor, ec, cs, blocker, timer)
  }  
}


