package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

sealed abstract class UserResponse

object UserResponse {

  case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse
  case class FailedToAuthenticate(msg: String) extends UserResponse

  // implicit lazy val authenticatedUserCodec: Codec[AuthenticatedUser] = deriveCodec
  // implicit lazy val failedToAuthenticateCodec: Codec[FailedToAuthenticate] = deriveCodec
  // implicit lazy val userResponseCodec: Codec[UserResponse] = deriveCodec
}
