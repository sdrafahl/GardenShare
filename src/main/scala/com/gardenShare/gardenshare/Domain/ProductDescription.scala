package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ProductDescription(name: String, priceUnit: PriceUnit, product: Produce)

object ProductDescription {
  implicit lazy final val productDescriptionCodec: Codec[ProductDescription] = deriveCodec
}
