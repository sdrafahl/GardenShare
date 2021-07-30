package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class ErrorResponse(message: String)

object ErrorResponse {
  implicit lazy final val codecErrorResponse: Codec[ErrorResponse] = deriveCodec
}
