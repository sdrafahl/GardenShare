package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class UserCreationRespose(msg: String, userCreated: Boolean)

object UserCreationRespose {
  implicit lazy final val userCreationResponseCodec: Codec[UserCreationRespose] = deriveCodec
}
