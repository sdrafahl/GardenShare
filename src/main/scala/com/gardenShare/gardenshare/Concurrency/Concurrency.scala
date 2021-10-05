package com.gardenShare.gardenshare

import cats.effect.IO
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

object ConcurrencyHelper {
  def createConcurrencyValues(threadCount: Int) = {
    lazy val executor = Executors.newWorkStealingPool(threadCount)
    lazy val ec = ExecutionContext.fromExecutor(executor)
    (executor, ec)
  }  
}


