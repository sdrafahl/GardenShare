package com.gardenShare.gardenshare

abstract class UserResponse
case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse
case class FailedToAuthenticate(msg: String) extends UserResponse
