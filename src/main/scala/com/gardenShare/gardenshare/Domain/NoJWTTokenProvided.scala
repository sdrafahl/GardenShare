package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class NoJWTTokenProvided(msg: String = "No JWT token provided")

object NoJWTTokenProvided {
  implicit lazy final val NoJWTTokenProvidedCodec: Codec[NoJWTTokenProvided] = deriveCodec
}
