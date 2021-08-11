package com.gardenShare.gardenshare

sealed abstract class UserResponse

object UserResponse {
  case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse
  case class FailedToAuthenticate(msg: String) extends UserResponse
}
