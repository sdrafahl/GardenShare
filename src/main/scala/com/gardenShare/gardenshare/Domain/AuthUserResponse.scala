package com.gardenShare.gardenshare

import UserResponse.AuthenticatedUser
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AuthUserResponse(
  msg: String,
  auth: Option[AuthenticatedUser],
  authenticated: Boolean
)

object AuthUserResponse {
  implicit lazy final val authUserResponseCodec: Codec[AuthUserResponse] = deriveCodec
}
