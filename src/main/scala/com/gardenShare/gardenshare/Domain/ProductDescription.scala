package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.extras.semiauto._

case class ProductDescription(name: String, priceUnit: PriceUnit, product: Produce)

object ProductDescription {
  implicit lazy final val ProductDescriptionCodec: Codec[ProductDescription] = deriveCodec
}
