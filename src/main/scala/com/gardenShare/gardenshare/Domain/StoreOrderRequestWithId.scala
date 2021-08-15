package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class StoreOrderRequestWithId(id: Int, storeOrderRequest: StoreOrderRequest)

object StoreOrderRequestWithId {
  implicit lazy final val storeOrderRequestWithIdCodec: Codec[StoreOrderRequestWithId] = deriveCodec
}
