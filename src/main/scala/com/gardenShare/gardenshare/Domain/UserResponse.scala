package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

sealed abstract class UserResponse

case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse

case class FailedToAuthenticate(msg: String) extends UserResponse

object UserResponse {
  implicit lazy final val userResponseCodec: Codec[UserResponse] = deriveCodec
}
