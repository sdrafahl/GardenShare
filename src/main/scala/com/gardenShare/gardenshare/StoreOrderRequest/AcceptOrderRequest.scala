package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import cats.effect.ContextShift
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.Storage.Relational.GetStoreOrderRequestsWithSellerEmail
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.GetStoreOrderRequestsWithBuyerEmail
import com.gardenShare.gardenshare.Storage.Relational.InsertIntoAcceptedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.Storage.Relational.InsertIntoDeniedStoreOrderRequestTableByID
import com.gardenShare.gardenshare.Storage.Relational.SearchDeniedStoreOrderRequestTable
import com.gardenShare.gardenshare.Storage.Relational.SearchAcceptedStoreOrderRequestTableByID
import cats.implicits._
import com.gardenShare.gardenshare.Storage.Relational.SearchStoreOrderRequestTable

abstract class AcceptOrderRequest[F[_]] {
  def accept(storeOrderIdToAccept: Int)(implicit cs: ContextShift[F]):F[Unit]
}

object AcceptOrderRequest {
  implicit def createIOAcceptOrderRequest(implicit in:InsertIntoAcceptedStoreOrderRequestTableByID[IO]) = new AcceptOrderRequest[IO] {
    def accept(storeOrderIdToAccept: Int)(implicit cs: ContextShift[IO]):IO[Unit] = in.insert(storeOrderIdToAccept).map(_ => ())
  }
}

abstract class DeniedOrderRequests[F[_]] {
  def deny(storeOrderToDeny: Int)(implicit cs: ContextShift[F]):F[Unit]
}

object DeniedOrderRequests {
  implicit def createIODeniedOrderRequests(implicit in: InsertIntoDeniedStoreOrderRequestTableByID[IO]) = new DeniedOrderRequests[IO] {
    def deny(storeOrderToDeny: Int)(implicit cs: ContextShift[IO]):IO[Unit] = in.insert(storeOrderToDeny).map(_ => ())
  }
}

abstract class StatusOfStoreOrderRequest[F[_]] {
  def get(id: Int)(implicit cs: ContextShift[F]): F[StoreOrderRequestStatus]
}

object StatusOfStoreOrderRequest {
  val timeTillExperiationInHours = 1
  implicit def createIOStatusOfStoreOrderRequest(implicit sa: SearchAcceptedStoreOrderRequestTableByID[IO], sd: SearchDeniedStoreOrderRequestTable[IO], parseDate: ParseDate, se:SearchStoreOrderRequestTable[IO], getTime: GetCurrentDate[IO]) = new StatusOfStoreOrderRequest[IO] {
    def get(id: Int)(implicit cs: ContextShift[IO]): IO[StoreOrderRequestStatus] = {
      for {
        a <- (sa.search(id), sd.search(id)).parBisequence
        status <- (a._1.headOption, a._2.headOption) match {
          case (_, Some(_)) => IO.pure(DeniedRequest)
          case (Some(_), None) => IO.pure(AcceptedRequest)
          case (None, None) => {
            se
              .search(id)
              .flatMap{
                case None => IO.raiseError[StoreOrderRequestWithId](new Throwable("Order does not exist"))
                case Some(a) => IO.pure(a)
              }
              .flatMap{storOrderWithId =>
                getTime
                  .get
                  .map{now =>
                    val dateSub = storOrderWithId.storeOrderRequest.dateSubmitted
                    val hourAfterSubmitting = dateSub.plusHours(timeTillExperiationInHours)                    
                    if((dateSub.isBefore(now) && (hourAfterSubmitting.isAfter(now) || hourAfterSubmitting.equals(now))) || dateSub.isAfter(now) || dateSub.equals(now)) {
                      RequestToBeDetermined
                    } else {
                      ExpiredRequest
                    }
                }
              }
          }
        }
      } yield status
    }
  }
}
