package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ProductWithId(id: ProductId, product: Product)

object ProductWithId {
  implicit lazy final val productWithIdCodec: Codec[ProductWithId] = deriveCodec
}
