package com.gardenShare.gardenshare

import cats.effect.IO
import cats.implicits._
import StoreOrderRequestStatus._

abstract class GetStatusOfStoreOrderRequest[F[_]] {
  def get(id: OrderId): F[StoreOrderRequestStatus]
}

object GetStatusOfStoreOrderRequest {
  private[this] val timeTillExperiationInHours = 1
  implicit def createIOStatusOfStoreOrderRequest(
      implicit searchAcceptedStoreOrderRequestTableByID: SearchAcceptedStoreOrderRequestTableByID[IO], // sa
      searchDeniedStoreOrderRequestTable: SearchDeniedStoreOrderRequestTable[IO], // sd
      searchStoreOrderRequestTable: SearchStoreOrderRequestTable[IO], // se
      getTime: GetCurrentDate[IO],
      orderIdIsPaidFor: OrderIdIsPaidFor[IO],
      searchSellerCompletedOrders: SellerCompleteOrders[IO],
      queryBuyerOrderComplete: QueryBuyerOrderComplete[IO]
  ) = new GetStatusOfStoreOrderRequest[IO] {
    def get(
        id: OrderId
    ): IO[StoreOrderRequestStatus] = {      
      for {        
        (acceptedOrders, denied, isPaidFor, sellerComplete, buyerComplete) <- (
          searchAcceptedStoreOrderRequestTableByID.search(id),
          searchDeniedStoreOrderRequestTable.search(id),
          orderIdIsPaidFor.isPaidFor(id),
          searchSellerCompletedOrders.search(id),
          queryBuyerOrderComplete.search(id)
        ).parMapN((a, b, c, d, e) => (a, b, c, d, e))
        status <- (acceptedOrders.headOption, denied.headOption, isPaidFor, sellerComplete, buyerComplete) match {
          case (_, _, _, _, Some(_))    => IO.pure(BuyerComplete)
          case (_, _, _, Some(_), _)    => IO.pure(SellerComplete)
          case (_, _, true, None, _)    => IO.pure(RequestPaidFor)
          case (_, Some(_), _, None, _)    => IO.pure(DeniedRequest)
          case (Some(_), None, _, None, _) => IO.pure(AcceptedRequest)
          case (None, None, _, None, _) => {
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
