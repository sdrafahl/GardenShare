package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class User(email: Email, password: Password)

object User {
  implicit lazy final val userCodec: Codec[User] = deriveCodec
}
