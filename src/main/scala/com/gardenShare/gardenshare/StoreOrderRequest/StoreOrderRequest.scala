package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Email
import java.time.ZonedDateTime

case class ProductAndQuantity(product: ProductWithId, quantity: Int)
case class StoreOrderRequest(seller: Email, buyer: Email, products: List[ProductAndQuantity], dateSubmitted: ZonedDateTime)
case class StoreOrderRequestWithId(id: Int, storeOrderRequest: StoreOrderRequest)

sealed abstract class StoreOrderRequestStatus
case object AcceptedRequest extends StoreOrderRequestStatus
case object DeniedRequest extends StoreOrderRequestStatus
case object ExpiredRequest extends StoreOrderRequestStatus
case object RequestToBeDetermined extends StoreOrderRequestStatus
case object RequestPaidFor extends StoreOrderRequestStatus
