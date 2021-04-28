package com.gardenShare.gardenshare

import cats.effect.IO
import java.time.ZonedDateTime
import cats.effect.ContextShift

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
