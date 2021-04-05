package com.gardenShare.gardenshare

case class AuthUserResponse(
  msg: String,
  auth: Option[AuthenticatedUser],
  authenticated: Boolean
)
