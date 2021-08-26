package com.gardenShare.gardenshare

import cats.effect.ContextShift
import com.gardenShare.gardenshare.Email
import cats.effect.IO
import com.gardenShare.gardenshare.InsertIntoAcceptedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.InsertIntoDeniedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.SearchStoreOrderRequestTable

abstract class AcceptOrderRequest[F[_]] {
  def accept(storeOrderIdToAccept: OrderId, sellerEmail: Email)(
      implicit cs: ContextShift[F]
  ): F[Unit]
}

object AcceptOrderRequest {
  implicit def createIOAcceptOrderRequest(
      implicit in: InsertIntoAcceptedStoreOrderRequestTableByID[IO],
      searchOrders: SearchStoreOrderRequestTable[IO]
  ) = new AcceptOrderRequest[IO] {
    def accept(storeOrderIdToAccept: OrderId, sellerEmail: Email)(
        implicit cs: ContextShift[IO]
    ): IO[Unit] = {
      for {
        order <- searchOrders.search(storeOrderIdToAccept)
        result <- order match {
          case None => IO.raiseError(new Throwable("Order does not exist"))
          case Some(order) => {
            order.storeOrderRequest.seller.equals(sellerEmail) match {
              case false =>
                IO.raiseError(
                  new Throwable("Order does not belong to that seller")
                )
              case true => in.insert(storeOrderIdToAccept).map(_ => ())
            }
          }
        }
      } yield result
    }
  }
}

abstract class DeniedOrderRequests[F[_]] {
  def deny(storeOrderToDeny: OrderId, sellerEmail: Email)(
      implicit cs: ContextShift[F]
  ): F[Unit]
}

object DeniedOrderRequests {
  implicit def createIODeniedOrderRequests(
      implicit in: InsertIntoDeniedStoreOrderRequestTableByID[IO],
      searchOrders: SearchStoreOrderRequestTable[IO]
  ) = new DeniedOrderRequests[IO] {
    def deny(storeOrderToDeny: OrderId, sellerEmail: Email)(
        implicit cs: ContextShift[IO]
    ): IO[Unit] = {
      for {
        order <- searchOrders.search(storeOrderToDeny)
        result <- order match {
          case None => IO.raiseError(new Throwable("Order does not exist"))
          case Some(order) => {
            order.storeOrderRequest.seller.equals(sellerEmail) match {
              case false =>
                IO.raiseError(
                  new Throwable("Order does not belong to that seller")
                )
              case true => in.insert(storeOrderToDeny).map(_ => ())
            }
          }
        }
      } yield result
    }
  }
}
