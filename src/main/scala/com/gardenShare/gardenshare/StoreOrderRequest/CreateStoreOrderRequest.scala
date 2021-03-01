package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.UserEntities.Email
import cats.effect.ContextShift
import com.gardenShare.gardenshare.Storage.Relational.InsertStoreOrderRequest

abstract class CreateStoreOrderRequest[F[_]] {
  def createOrder(seller: Email, buyer: Email, products: List[ProductWithId])(implicit cs: ContextShift[F], gd: GetCurrentDate[F]): F[StoreOrderRequestWithId]
}

object CreateStoreOrderRequest {
  def apply[F[_]: CreateStoreOrderRequest]() = implicitly[CreateStoreOrderRequest[F]]

  implicit def createIOCreateStoreOrderRequest(implicit i:InsertStoreOrderRequest[IO], g: GetCurrentDate[IO], gs: GetStoreOrderRequestsWithinTimeRangeOfBuyer[IO]) = new CreateStoreOrderRequest[IO] {
    def createOrder(seller: Email, buyer: Email, products: List[ProductWithId])(implicit cs: ContextShift[IO], gd: GetCurrentDate[IO]): IO[StoreOrderRequestWithId] = {
      val waitTimeInMinutes = 1
      for {
        currentDate <- gd.get        
        past = currentDate.minusMinutes(waitTimeInMinutes)
        requestsBelongToBuyer <- gs.getStoreOrdersWithin(past, currentDate, buyer)
        _ <- (requestsBelongToBuyer.find(p => p.storeOrderRequest.seller.equals(seller))) match {
          case Some(_) => IO.raiseError(new Throwable(s"Request already created between the seller and buyer, please wait at most ${waitTimeInMinutes} minutes"))           
          case None => IO.pure(())
        }
        req = StoreOrderRequest(seller, buyer, products, currentDate)
        res <- i.insertStoreOrderRequest(req)
      } yield res
    }
  }
}
