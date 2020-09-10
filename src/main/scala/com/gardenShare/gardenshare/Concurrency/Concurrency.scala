package com.gardenShare.gardenshare.Concurrency

import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import cats.effect.ContextShift
import cats.effect.Blocker

object Concurrency {
  implicit private val executor = Executors.newCachedThreadPool()
  implicit private val ec = ExecutionContext.fromExecutor(executor)
  implicit lazy val cs = IO.contextShift(ec)  
  implicit lazy val blocker = Blocker.liftExecutorService(executor)

}
