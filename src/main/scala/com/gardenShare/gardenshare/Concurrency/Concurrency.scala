package com.gardenShare.gardenshare

import cats.effect.IO
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import cats.effect.Blocker

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


