package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class StoreOrderRequestStatusBody(response: StoreOrderRequestStatus)

object StoreOrderRequestStatusBody {
  implicit lazy final val storeOrderRequestStatusBodyCodec: Codec[StoreOrderRequestStatusBody] = deriveCodec
}
