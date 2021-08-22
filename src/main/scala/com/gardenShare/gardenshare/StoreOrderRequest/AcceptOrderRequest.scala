package com.gardenShare.gardenshare

import cats.effect.ContextShift
import com.gardenShare.gardenshare.Email
import cats.effect.IO
import com.gardenShare.gardenshare.InsertIntoAcceptedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.InsertIntoDeniedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.SearchDeniedStoreOrderRequestTable
import com.gardenShare.gardenshare.SearchAcceptedStoreOrderRequestTableByID
import cats.implicits._
import com.gardenShare.gardenshare.SearchStoreOrderRequestTable
import StoreOrderRequestStatus._

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

abstract class StatusOfStoreOrderRequest[F[_]] {
  def get(id: OrderId)(implicit cs: ContextShift[F]): F[StoreOrderRequestStatus]
}

object StatusOfStoreOrderRequest {
  private[this] val timeTillExperiationInHours = 1
  implicit def createIOStatusOfStoreOrderRequest(
      implicit searchAcceptedStoreOrderRequestTableByID: SearchAcceptedStoreOrderRequestTableByID[IO], // sa
      searchDeniedStoreOrderRequestTable: SearchDeniedStoreOrderRequestTable[IO], // sd
      searchStoreOrderRequestTable: SearchStoreOrderRequestTable[IO], // se
      getTime: GetCurrentDate[IO],
      orderIdIsPaidFor: OrderIdIsPaidFor[IO],
      searchSellerCompletedOrders: SellerCompleteOrders[IO]
  ) = new StatusOfStoreOrderRequest[IO] {
    def get(
        id: OrderId
    )(implicit cs: ContextShift[IO]): IO[StoreOrderRequestStatus] = {      
      for {
        _ <- searchAcceptedStoreOrderRequestTableByID.search(id)
        (acceptedOrders, denied, isPaidFor, sellerComplete) <- (
          searchAcceptedStoreOrderRequestTableByID.search(id),
          searchDeniedStoreOrderRequestTable.search(id),
          orderIdIsPaidFor.isPaidFor(id),
          searchSellerCompletedOrders.search(id)
        ).parMapN((a, b, c, d) => (a, b, c, d))
        status <- (acceptedOrders.headOption, denied.headOption, isPaidFor, sellerComplete) match {
          case (_, _, _, Some(_))    => IO.pure(SellerComplete)
          case (_, _, true, None)    => IO.pure(RequestPaidFor)
          case (_, Some(_), _, None)    => IO.pure(DeniedRequest)
          case (Some(_), None, _, None) => IO.pure(AcceptedRequest)
          case (None, None, _, None) => {
            for {
              maybeStoreOrderRequestWithId <- searchStoreOrderRequestTable.search(id)
              storeOrderRequestWithId <- IO.fromOption(maybeStoreOrderRequestWithId)(new Throwable("Order does not exist"))
              currentTime <- getTime.get
              dateSubmitted = storeOrderRequestWithId.storeOrderRequest.dateSubmitted
              hourAfterSubmitted = dateSubmitted.plusHours(timeTillExperiationInHours)              
            } yield if(
              dateSubmitted.isBefore(currentTime) &&
              hourAfterSubmitted.isAfter(currentTime) ||
              hourAfterSubmitted.equals(currentTime) ||
              dateSubmitted.isAfter(currentTime) ||
                dateSubmitted.equals(currentTime)) {
              RequestToBeDetermined
            } else {
              ExpiredRequest
            }            
          }
        }
      } yield status
    }
  }
}
