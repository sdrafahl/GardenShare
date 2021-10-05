package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.InsertStoreOrderRequest
import scala.concurrent.ExecutionContext

abstract class CreateStoreOrderRequest[F[_]] {
  def createOrder(seller: Email, buyer: Email, products: List[ProductAndQuantity])(implicit gd: GetCurrentDate[F], ec: ExecutionContext): F[StoreOrderRequestWithId]
}

object CreateStoreOrderRequest {
  def apply[F[_]: CreateStoreOrderRequest]() = implicitly[CreateStoreOrderRequest[F]]

  implicit def createIOCreateStoreOrderRequest(implicit i:InsertStoreOrderRequest[IO], gs: GetStoreOrderRequestsWithinTimeRangeOfBuyer[IO]) = new CreateStoreOrderRequest[IO] {
    def createOrder(seller: Email, buyer: Email, products: List[ProductAndQuantity])(implicit gd: GetCurrentDate[IO], ec: ExecutionContext): IO[StoreOrderRequestWithId] = {
      val waitTimeInMinutes = 0
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
