package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import cats.effect.ContextShift
import com.gardenShare.gardenshare.Email
import cats.effect.IO
import com.gardenShare.gardenshare.InsertIntoAcceptedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.InsertIntoDeniedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.SearchDeniedStoreOrderRequestTable
import com.gardenShare.gardenshare.SearchAcceptedStoreOrderRequestTableByID
import cats.implicits._
import com.gardenShare.gardenshare.SearchStoreOrderRequestTable

abstract class AcceptOrderRequest[F[_]] {
  def accept(storeOrderIdToAccept: Int, sellerEmail: Email)(
      implicit cs: ContextShift[F]
  ): F[Unit]
}

object AcceptOrderRequest {
  implicit def createIOAcceptOrderRequest(
      implicit in: InsertIntoAcceptedStoreOrderRequestTableByID[IO],
      searchOrders: SearchStoreOrderRequestTable[IO],
      pd: Parser[ZonedDateTime]
  ) = new AcceptOrderRequest[IO] {
    def accept(storeOrderIdToAccept: Int, sellerEmail: Email)(
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
  def deny(storeOrderToDeny: Int, sellerEmail: Email)(
      implicit cs: ContextShift[F]
  ): F[Unit]
}

object DeniedOrderRequests {
  implicit def createIODeniedOrderRequests(
      implicit in: InsertIntoDeniedStoreOrderRequestTableByID[IO],
      searchOrders: SearchStoreOrderRequestTable[IO],
      pd: Parser[ZonedDateTime]
  ) = new DeniedOrderRequests[IO] {
    def deny(storeOrderToDeny: Int, sellerEmail: Email)(
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
  def get(id: Int)(implicit cs: ContextShift[F]): F[StoreOrderRequestStatus]
}

object StatusOfStoreOrderRequest {
  val timeTillExperiationInHours = 1
  implicit def createIOStatusOfStoreOrderRequest(
      implicit sa: SearchAcceptedStoreOrderRequestTableByID[IO],
      sd: SearchDeniedStoreOrderRequestTable[IO],
      parseDate: Parser[ZonedDateTime],
      se: SearchStoreOrderRequestTable[IO],
      getTime: GetCurrentDate[IO],
      orderIdIsPaidFor: OrderIdIsPaidFor[IO]
  ) = new StatusOfStoreOrderRequest[IO] {
    def get(
        id: Int
    )(implicit cs: ContextShift[IO]): IO[StoreOrderRequestStatus] = {
      (sa.search(id), sd.search(id), orderIdIsPaidFor.isPaidFor(id)).parMapN {
        (acceptedOrders, denied, isPaidFor) =>
        for {
            status <- (acceptedOrders.headOption, denied.headOption, isPaidFor) match {
              case (_, _, true)       => IO.pure(RequestPaidFor)
              case (_, Some(_), _)    => IO.pure(DeniedRequest)
              case (Some(_), None, _) => IO.pure(AcceptedRequest)
              case (None, None, _) => {
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
