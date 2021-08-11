package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class InvalidLimitProvided(msg: String)

object InvalidLimitProvided {
  implicit lazy final val InvalidLimitProvidedCodec: Codec[InvalidLimitProvided] = deriveCodec
}
