package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class CreateProductRequest(storeId: Int, product: Produce, am: Amount)

object CreateProductRequest {
  implicit lazy final val createProductRequestCodec: Codec[CreateProductRequest] = deriveCodec
}
