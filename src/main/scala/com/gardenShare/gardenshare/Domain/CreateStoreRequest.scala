package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class CreateStoreRequest(address: Address, sellerEmail: Email)

object CreateStoreRequest {
  implicit lazy final val createStoreRequestCodec: Codec[CreateStoreRequest] = deriveCodec
}
