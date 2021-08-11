package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.GetStoresStream
import cats.syntax.all._
import cats.effect.{IO, Timer}
import fs2.concurrent.InspectableQueue
import cats.Monad
import scala.concurrent.duration._
import com.gardenShare.gardenshare.IsWithinRange.Ops
import cats.effect.ContextShift

abstract class GetNearestStores[F[_]] {
  def getNearest(n: DistanceInMiles, limit: Limit, fromLocation: Address)(
    implicit getDist: GetDistance[F],
    getStores: GetStoresStream[F],
    cs: ContextShift[F],
    threadCount: GetThreadCountForFindingNearestStores[F],
    timer: Timer[F]
  ): F[List[RelativeDistanceAndStore]]
}
object GetNearestStores {
  def apply[F[_]: GetNearestStores]() = implicitly[GetNearestStores[F]]

  private def isWithinRange[F[_]:Monad](range: DistanceInMiles, from: Address, to: Address, getDist: GetDistance[F]): F[(Boolean, DistanceInMiles)] = {
    for {
      dist <- getDist.getDistanceFromAddress(from, to)
    } yield (dist.inRange(range), dist)
  }
  implicit object IOGetNearestStore extends GetNearestStores[IO] {
    def getNearest(n: DistanceInMiles, limit: Limit, fromLocation: Address)(
      implicit getDist: GetDistance[IO],
      getStores: GetStoresStream[IO],
      cs: ContextShift[IO],
      threadCount: GetThreadCountForFindingNearestStores[IO],
      timer: Timer[IO]
    ): IO[List[RelativeDistanceAndStore]] = {
      val stores = getStores.getLazyStores
      for {
        queue <- InspectableQueue.bounded[IO, RelativeDistanceAndStore](limit.l)
        threads <- threadCount.get
        _ <- for {
          processQueue <- stores.parEvalMap(threads) {
            store => {
                isWithinRange[IO](n, fromLocation, store.address, getDist).flatMap {
                  case (true, dist) => {
                    queue.getSize.flatMap{ depth =>
                      if(depth < limit.l) {
                        queue.enqueue1(RelativeDistanceAndStore(store, dist))
                      } else {
                        IO.raiseError(new Throwable("Done adding to queue"))
                      }
                    }
                    
                  }
                  case (false, _) => IO.unit
                }
             }
          }
          .compile
          .toList
          .timeout(5 seconds)
          .attempt
        } yield processQueue
        dep <- queue.getSize
        listOfStores <- queue.dequeue.take(dep).compile.toList
       } yield listOfStores
    }
  }
  implicit class GetNearestOps(underlying: GetNearestStore) {
    def nearest[F[_]:GetDistance:GetStoresStream:ContextShift:Timer:GetThreadCountForFindingNearestStores](implicit getNearest: GetNearestStores[F]) =
      getNearest.getNearest(underlying.n, underlying.limit, underlying.fromLocation)
  }
}
