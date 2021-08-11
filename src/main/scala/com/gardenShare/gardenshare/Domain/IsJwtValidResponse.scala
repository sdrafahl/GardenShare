package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class IsJwtValidResponse(msg: String, valid: Boolean)

object IsJwtValidResponse {
  implicit lazy final val IsJwtValidResponseCodec: Codec[IsJwtValidResponse] = deriveCodec
}
