package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AddressNotProvided(msg: String = "Please Provide an Address")

object AddressNotProvided {
  implicit lazy final val addressCodec: Codec[Address] = deriveCodec
}
