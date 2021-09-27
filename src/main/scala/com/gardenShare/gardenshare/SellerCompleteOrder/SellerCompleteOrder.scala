package com.gardenShare.gardenshare

import cats.effect.IO
import cats.implicits._
import com.gardenShare.gardenshare.StoreOrderRequestStatus._

abstract class SellerCompleteOrder[F[_]] {
  def completeOrder(request: SellerCompleteOrderRequest)(implicit cs: ContextShift[F]): F[Unit]
}

object SellerCompleteOrder {
  def apply[F[_]: SellerCompleteOrder]() = implicitly[SellerCompleteOrder[F]]

  implicit def createIOSellerCompleteOrder(
    implicit getStatusOfOrder: GetStatusOfStoreOrderRequest[IO],
    searchForOrder: SearchStoreOrderRequestTable[IO],
    insertOrderIntoAcceptedTable: InsertOrderIntoCompleteTable[IO]
  ) = new SellerCompleteOrder[IO] {
    def completeOrder(request: SellerCompleteOrderRequest): IO[Unit] = {
      for {
        (statusOfOrder, order) <- (getStatusOfOrder.get(request.orderID), searchForOrder.search(request.orderID)).parBisequence
        _ <- (statusOfOrder, order) match {
          case (_, None) => IO.raiseError(new Throwable("Order Not found"))
          case (AcceptedRequest, _) => IO.raiseError(new Throwable("Order needs to be paid for"))
          case (DeniedRequest, _) => IO.raiseError(new Throwable("Order is Denied"))
          case (ExpiredRequest, _) => IO.raiseError(new Throwable("Order is expired"))
          case (RequestToBeDetermined, _) => IO.raiseError(new Throwable("Request is not accepted yet"))
          case (SellerComplete, _) => IO.raiseError(new Throwable("Order is already complete"))
          case (RequestPaidFor, Some(order)) if order.storeOrderRequest.seller.equals(request.seller) => insertOrderIntoAcceptedTable.insertOrder(order.id)
          case _ => IO.raiseError(new Throwable("Order does not belong to seller"))
        }
      } yield ()
    }
  }
  implicit class SellerCompleteOrderOps(underlying: SellerCompleteOrderRequest) {
    def complete[F[_]: SellerCompleteOrder:ContextShift] = implicitly[SellerCompleteOrder[F]].completeOrder(underlying)
  }
}
