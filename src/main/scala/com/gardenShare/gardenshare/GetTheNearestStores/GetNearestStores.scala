package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.domain.Store.Address
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import com.gardenShare.gardenshare.domain.Store.Store
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream
import cats.effect.IO
import fs2.concurrent.Queue
import cats.syntax.all._
import cats.effect.{Concurrent, ExitCode, IO, IOApp, Timer}
import fs2.concurrent.InspectableQueue
import fs2.Stream
import scala.concurrent.duration._
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.Config.GetGoogleMapsApiKey
import cats.Monad
import com.gardenShare.gardenshare.GoogleMapsClient.IsWithinRange._
import scala.concurrent.duration._
import cats.effect.concurrent.Ref
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance._
import com.gardenShare.gardenshare.GoogleMapsClient.DistanceInMiles

abstract class GetNearestStores[F[_]] {
  def getNearest(n: DistanceInMiles, limit: Int, fromLocation: Address)(implicit getDist: GetDistance[F], getStores: GetStoresStream[F]): F[List[Store]]
}

case class GetNearestStore(n: DistanceInMiles, limit: Int, fromLocation: Address)

object GetNearestStores {
  def apply[F[_]: GetNearestStores]() = implicitly[GetNearestStores[F]]

  private def isWithinRange[F[_]: GetDistance:GetGoogleMapsApiKey:Monad](range: DistanceInMiles, from: Address, to: Address, getDist: GetDistance[F]): F[Boolean] = {
    for {
      dist <- getDist.getDistanceFromAddress(from, to)
    } yield dist.inRange(range)
  }
  implicit object IOGetNearestStore extends GetNearestStores[IO] {
    def getNearest(n: DistanceInMiles, limit: Int, fromLocation: Address)(implicit getDist: GetDistance[IO], getStores: GetStoresStream[IO]): IO[List[Store]] = {
      val stores = getStores.getLazyStores()

      for {
        queue <- InspectableQueue.bounded[IO, Store](limit)
        populateQueue <- for {
          processQueue <- stores.parEvalMap(com.gardenShare.gardenshare.Concurrency.Concurrency.threadCount) {
            store => {
                isWithinRange[IO](n, fromLocation, store.address, getDist).flatMap {
                  case true => {
                    queue.getSize.flatMap{ depth =>
                      if(depth < limit) {
                        queue.enqueue1(store)
                      } else {
                        IO.raiseError(new Throwable("Done adding to queue"))
                      }
                    }
                    
                  }
                  case false => IO.unit
                }
             }
          }
          .compile
          .toList
          .timeout(15 seconds)
          .attempt
        } yield processQueue        
        listOfStores <- queue.dequeue.take(limit).compile.toList
       } yield listOfStores
    }
  }
  implicit class GetNearestOps(underlying: GetNearestStore) {
    def nearest[F[_]: GetNearestStores:GetDistance:GetStoresStream](implicit getNearest: GetNearestStores[F]) = getNearest.getNearest(underlying.n, underlying.limit, underlying.fromLocation)
  }

}
