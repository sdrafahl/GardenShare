package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.GetStoresStream
import cats.syntax.all._
import cats.effect.IO
import cats.effect.std.Dequeue
import cats.effect.std.Queue
import scala.concurrent.duration._
import com.gardenShare.gardenshare.IsWithinRange.Ops
import cats.effect.Temporal
import cats.Monad
import fs2.Chunk
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import scala.annotation.tailrec

abstract class GetNearestStores[F[_]] {
  def getNearest(n: DistanceInMiles, limit: Limit, fromLocation: Address)(
    implicit getDist: GetDistance[F],
    getStores: GetStoresStream[F],
    threadCount: GetThreadCountForFindingNearestStores[F],
    timer: Temporal[F]
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
      threadCount: GetThreadCountForFindingNearestStores[IO],
      timer: Temporal[IO]
    ): IO[List[RelativeDistanceAndStore]] = {
      val stores: Stream[IO, Store] = getStores.getLazyStores
      for {
        threads <- threadCount.get
        storesAndWithinRange = stores.parEvalMap(threads) {
          store => {
            for {
              (isWithinRange, distance) <- isWithinRange[IO](n, fromLocation, store.address, getDist)              
            } yield (isWithinRange, distance, store)
          }
        }
        storesWithinRange <- storesAndWithinRange
        .filter(withinRangeAndStore => withinRangeAndStore._1)
        .parEvalMap(threads)(withinRangeAndStore => IO.pure(RelativeDistanceAndStore(withinRangeAndStore._3, withinRangeAndStore._2)))
        .take(limit.l)
        .compile
        .toList        
      } yield storesWithinRange
    }
  }

  private[this] def unloadQueue[A, F[_]: Monad](amountToUnload: Int, queue: Queue[F, A], acc: List[A] = List()): F[List[A]] = { // refactor this later or find a different way
    if(amountToUnload == 0) {
      Monad[F].pure(acc)
    } else {
      for {
        business <- queue.tryTake
        newAcc = acc ::: business.toList
        result <- unloadQueue(amountToUnload, queue, newAcc)
      } yield result
    }
    
 }

  implicit class GetNearestOps(underlying: GetNearestStore) {
    def nearest[F[_]:GetDistance:GetStoresStream:Temporal:GetThreadCountForFindingNearestStores](implicit getNearest: GetNearestStores[F]) =
      getNearest.getNearest(underlying.n, underlying.limit, underlying.fromLocation)
  }
}
