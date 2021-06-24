package com.gardenShare.gardenshare

import cats.Show
import cats.kernel.Hash
import cats.kernel.Order
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class StoreOrderRequestBody(body: List[ProductAndQuantity])

object StoreOrderRequestBody {
  implicit lazy final val storeOrderRequestCodec: Codec[ProductAndQuantity] = deriveCodec
}
