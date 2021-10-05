package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import cats.effect.IO

abstract class GetStoreOrderRequestsWithinTimeRangeOfSeller[F[_]] {
  def getStoreOrdersWithin(from: ZonedDateTime, to: ZonedDateTime, email: Email): F[List[StoreOrderRequestWithId]]
}

object GetStoreOrderRequestsWithinTimeRangeOfSeller {
  implicit def createIOGetStoreOrderRequestsWithinTimeRangeOfSeller(implicit getSellerOrders: GetStoreOrderRequestsWithSellerEmail[IO]) = new GetStoreOrderRequestsWithinTimeRangeOfSeller[IO] {
    def getStoreOrdersWithin(from: ZonedDateTime, to: ZonedDateTime, email: Email) = {
      for {
        allOrders <- getSellerOrders.getWithEmail(email)
        ordersWithinRanges = allOrders.filter{order =>
          val submitted = order.storeOrderRequest.dateSubmitted
          (from.isBefore(submitted) || from.equals(submitted)) && (to.isAfter(submitted) || to.equals(submitted))
        }
      } yield ordersWithinRanges
    }
  }
}
