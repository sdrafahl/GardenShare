package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class InvalidRangeProvided(msg: String)

object InvalidRangeProvided {
  implicit lazy final val InvalidRangeProvidedCodec: Codec[InvalidRangeProvided] = deriveCodec
}
