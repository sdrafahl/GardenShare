package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import cats.effect.ContextShift
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.Storage.Relational.GetStoreOrderRequestsWithSellerEmail
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.GetStoreOrderRequestsWithBuyerEmail

abstract class GetStoreOrderRequestsWithinTimeRangeOfSeller[F[_]] {
  def getStoreOrdersWithin(from: ZonedDateTime, to: ZonedDateTime, email: Email)(implicit cs: ContextShift[F]): F[List[StoreOrderRequestWithId]]
}

object GetStoreOrderRequestsWithinTimeRangeOfSeller {
  implicit def createIOGetStoreOrderRequestsWithinTimeRangeOfSeller(implicit getSellerOrders: GetStoreOrderRequestsWithSellerEmail[IO]) = new GetStoreOrderRequestsWithinTimeRangeOfSeller[IO] {
    def getStoreOrdersWithin(from: ZonedDateTime, to: ZonedDateTime, email: Email)(implicit cs: ContextShift[IO]) = {
      getSellerOrders
        .getWithEmail(email)
        .map(_.filter{ab =>
          val submitted = ab.storeOrderRequest.dateSubmitted
          (from.isBefore(submitted) || from.equals(submitted)) && (to.isAfter(submitted) || to.equals(submitted))
        })
    }
  }
}

abstract class GetStoreOrderRequestsWithinTimeRangeOfBuyer[F[_]] {
  def getStoreOrdersWithin(from: ZonedDateTime, to: ZonedDateTime, email: Email)(implicit cs: ContextShift[F]): F[List[StoreOrderRequestWithId]]
}

object GetStoreOrderRequestsWithinTimeRangeOfBuyer {
  implicit def createIOGetStoreOrderRequestsWithinTimeRangeOfBuyer(implicit getBuyerOrders: GetStoreOrderRequestsWithBuyerEmail[IO]) = new GetStoreOrderRequestsWithinTimeRangeOfBuyer[IO] {
    def getStoreOrdersWithin(from: ZonedDateTime, to: ZonedDateTime, email: Email)(implicit cs: ContextShift[IO]) = {
      getBuyerOrders
        .getWithEmail(email)
        .map(_.filter{ab =>
          val submitted = ab.storeOrderRequest.dateSubmitted
          (from.isBefore(submitted) || from.equals(submitted)) && (to.isAfter(submitted) || to.equals(submitted))
        })
    }
  }
}
