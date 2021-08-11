package com.gardenShare.gardenshare.GardenData

import io.circe.Codec
import io.circe.generic.extras.semiauto._

case class Plant(name: String) extends AnyVal

object Plant {
  implicit lazy final val plantCodec: Codec[Plant] = deriveUnwrappedCodec
}
