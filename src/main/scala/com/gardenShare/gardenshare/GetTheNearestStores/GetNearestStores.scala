package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.domain.Store.Address
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import com.gardenShare.gardenshare.domain.Store.Store
import com.gardenShare.gardenshare.GoogleMapsClient.Distance
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream
import cats.effect.IO
import fs2.concurrent.Queue
import cats.syntax.all._
import cats.effect.{Concurrent, ExitCode, IO, IOApp, Timer}
import fs2.concurrent.Queue
import fs2.Stream
import scala.concurrent.duration._
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.Config.GetGoogleMapsApiKey
import cats.Monad
import com.gardenShare.gardenshare.GoogleMapsClient.IsWithinRange._
import scala.concurrent.duration._
import cats.effect.concurrent.Ref

abstract class GetNearestStores[F[_]] {
  def getNearest(n: Distance, limit: Int, fromLocation: Address)(implicit getDist: GetDistance[F], getStores: GetStoresStream[F]): F[List[Store]]
}

object GetNearestStores {
  def apply[F[_]: GetNearestStores]() = implicitly[GetNearestStores[F]]
  private def isWithinRange[F[_]: GetDistance:GetGoogleMapsApiKey:Monad](range: Distance, from: Address, to: Address, getDist: GetDistance[F]): F[Boolean] = {
    for {
      dist <- getDist.getDistanceFromAddress(from, to)
    } yield dist.inRange(range)
  }
  implicit object IOGetNearestStore extends GetNearestStores[IO] {
    def getNearest(n: Distance, limit: Int, fromLocation: Address)(implicit getDist: GetDistance[IO], getStores: GetStoresStream[IO]): IO[List[Store]] = {
      val stores = getStores.getLazyStores()     
      for {
        queue <- Queue.bounded[IO, Store](limit)
        stackDepth <- Ref[IO].of(0)
        populateQueue <- for {          
          approximateDepth <- stackDepth.get
          processQueue = stores.parEvalMap(com.gardenShare.gardenshare.Concurrency.Concurrency.threadCount) {
            store =>
            if(approximateDepth < limit) { 
                isWithinRange[IO](n, fromLocation, store.address, getDist).map {
                  case true => queue.enqueue1(store).map(_ => stackDepth.modify(d => (d + 1, d))).flatMap(a => a)
                  case false => IO.unit                
                }.flatMap(a => a)
              } else { IO.unit }
            }.compile.drain
        } yield processQueue
        checkQueueProgram = {
          def checkQueue: IO[Unit] = {
            (for {
              aproxDepth <- stackDepth.get
            } yield aproxDepth match {
              case d if d < limit => {
                Thread.sleep(1000)
                checkQueue
              }
              case _ => IO.unit
            }).flatMap(a => a)
          }
          checkQueue
        }
        _ <- IO.race(populateQueue, IO.race(checkQueueProgram, IO.sleep(1 minute)))
        listOfStores <- queue.dequeueChunk(limit).mapAsync(com.gardenShare.gardenshare.Concurrency.Concurrency.threadCount) { a =>IO(List(a)) }
        .compile
        .fold(List[Store]()) { (a,b) => a ++ b }        
      } yield listOfStores
    }
  }

}
