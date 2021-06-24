package com.gardenShare.gardenshare

import cats.Show
import cats.kernel.Hash
import cats.kernel.Order
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ProductWithId(id: Int, product: Product)

object ProductWithId {
  implicit lazy final val productWithIdCodec: Codec[ProductWithId] = deriveCodec
}
