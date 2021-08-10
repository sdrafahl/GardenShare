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
              case true => in.insert(storeOrderIdToAccept.id).map(_ => ())
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
              case true => in.insert(storeOrderToDeny.id).map(_ => ())
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
  val timeTillExperiationInHours = 1
  implicit def createIOStatusOfStoreOrderRequest(
      implicit sa: SearchAcceptedStoreOrderRequestTableByID[IO],
      sd: SearchDeniedStoreOrderRequestTable[IO],
      se: SearchStoreOrderRequestTable[IO],
      getTime: GetCurrentDate[IO],
      orderIdIsPaidFor: OrderIdIsPaidFor[IO],
      searchCompletedOrders: SearchCompletedOrders[IO]
  ) = new StatusOfStoreOrderRequest[IO] {
    def get(
        id: OrderId
    )(implicit cs: ContextShift[IO]): IO[StoreOrderRequestStatus] = {
      (sa.search(id.id), sd.search(id.id), orderIdIsPaidFor.isPaidFor(id.id), searchCompletedOrders.search(id.id)).parMapN {
        (acceptedOrders, denied, isPaidFor, ordersComplete) =>
        for {
          status <- (acceptedOrders.headOption, denied.headOption, isPaidFor, ordersComplete) match {
              case (_, _, _, Some(_))    => IO.pure(SellerComplete)
              case (_, _, true, None)    => IO.pure(RequestPaidFor)
              case (_, Some(_), _, None)    => IO.pure(DeniedRequest)
              case (Some(_), None, _, None) => IO.pure(AcceptedRequest)
              case (None, None, _, None) => {
                se.search(id)
                  .flatMap {
                    case None =>
                      IO.raiseError[StoreOrderRequestWithId](
                        new Throwable("Order does not exist")
                      )
                    case Some(a) => IO.pure(a)
                  }
                  .flatMap { storOrderWithId =>
                    getTime.get
                      .map { now =>
                        val dateSub =
                          storOrderWithId.storeOrderRequest.dateSubmitted
                        val hourAfterSubmitting =
                          dateSub.plusHours(timeTillExperiationInHours)
                        if ((dateSub.isBefore(now) && (hourAfterSubmitting
                              .isAfter(now) || hourAfterSubmitting
                              .equals(now))) || dateSub.isAfter(now) || dateSub
                              .equals(now)) {
                          RequestToBeDetermined
                        } else {
                          ExpiredRequest
                        }                        
                      }
                  }
              }
            }
        } yield status
      }.flatten
    }
  }
}
