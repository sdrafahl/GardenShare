package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class UserInfo(email: Email, userType: UserType, store: Option[Store])

object UserInfo {
  implicit lazy final val userInfoCodec: Codec[UserInfo] = deriveCodec
}
