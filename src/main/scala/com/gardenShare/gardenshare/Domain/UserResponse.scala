package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed abstract class UserResponse

object UserResponse {
  case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse
  case class FailedToAuthenticate(msg: String) extends UserResponse

  implicit lazy final val authenticatedCodec: Codec[AuthenticatedUser] = deriveCodec
  implicit lazy final val userResponseCodec: Codec[UserResponse] = deriveCodec
}
