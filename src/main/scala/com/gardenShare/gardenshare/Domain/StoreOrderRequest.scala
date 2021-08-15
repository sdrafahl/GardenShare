package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class StoreOrderRequest(seller: Email, buyer: Email, products: List[ProductAndQuantity], dateSubmitted: ZonedDateTime)

object StoreOrderRequest {
  implicit lazy final val storeOrderRequestCodec: Codec[StoreOrderRequest] = deriveCodec
}
