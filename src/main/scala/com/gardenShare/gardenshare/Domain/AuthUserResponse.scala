package com.gardenShare.gardenshare

import UserResponse.AuthenticatedUser

case class AuthUserResponse(
  msg: String,
  auth: Option[AuthenticatedUser],
  authenticated: Boolean
)


