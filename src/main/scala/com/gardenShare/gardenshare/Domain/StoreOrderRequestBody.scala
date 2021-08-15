package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class StoreOrderRequestBody(body: List[ProductAndQuantity])

object StoreOrderRequestBody {
  implicit lazy final val storeOrderRequestCodec: Codec[StoreOrderRequestBody] = deriveCodec
}
