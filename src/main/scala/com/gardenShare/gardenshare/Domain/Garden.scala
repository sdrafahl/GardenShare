package com.gardenShare.gardenshare.GardenData

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class Garden(plants: List[Plant], owner: String)

object Garden {
  implicit lazy final val gardenCodec: Codec[Garden] = deriveCodec
}
