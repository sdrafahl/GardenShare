package com.gardenShare.gardenshare

import cats.Show
import cats.kernel.Hash
import cats.kernel.Order
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ProductAndQuantity(product: ProductWithId, quantity: Int)

object ProductAndQuantity {
  implicit lazy final val ProductAndQuantityCodec: Codec[ProductAndQuantity] = deriveCodec
}
