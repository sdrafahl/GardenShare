package com.gardenShare.gardenshare

import cats.effect.IO
import cats.implicits._
import StoreOrderRequestStatus._

abstract class BuyerCompleteOrder[F[_]] {
  def completeOrder(command: BuyerCompleteOrderRequest): F[Unit]
}

object BuyerCompleteOrder {
  implicit def createIOBuyerCompleteOrder(
    implicit getStatusOfStoreOrderRequest: GetStatusOfStoreOrderRequest[IO],
    searchForOrder: SearchStoreOrderRequestTable[IO],
    insertIntoBuyerOrderCompleteTable: InsertIntoBuyerOrderCompleteTable[IO]
  ) = new BuyerCompleteOrder[IO] {
    def completeOrder(command: BuyerCompleteOrderRequest): IO[Unit] = {
      for {
        (statusOfStoreOrderRequest, order) <- (getStatusOfStoreOrderRequest.get(command.orderID), searchForOrder.search(command.orderID)).parBisequence
        _ <- (statusOfStoreOrderRequest, order) match {
          case (_, None) => IO.raiseError(new Throwable("Order Not found"))
          case (AcceptedRequest, _) => IO.raiseError(new Throwable("Order needs to be paid for"))
          case (DeniedRequest, _) => IO.raiseError(new Throwable("Order is Denied"))
          case (ExpiredRequest, _) => IO.raiseError(new Throwable("Order is expired"))
          case (RequestToBeDetermined, _) => IO.raiseError(new Throwable("Request is not accepted yet"))
          case (SellerComplete, _) => IO.raiseError(new Throwable("Order is already complete"))
          case (RequestPaidFor, Some(order)) if order.storeOrderRequest.buyer.equals(command.buyer) => insertIntoBuyerOrderCompleteTable.insert(order.id)
          case _ => IO.raiseError(new Throwable("Order does not belong to seller"))
        }
      } yield ()
    }
  }

  implicit class BuyerCompleteOrderOps(underlying: BuyerCompleteOrderRequest) {
    def completeOrder[F[_]: BuyerCompleteOrder] = implicitly[BuyerCompleteOrder[F]].completeOrder(underlying)
  }

}
