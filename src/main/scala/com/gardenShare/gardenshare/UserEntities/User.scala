package com.gardenShare.gardenshare.UserEntities

case class Email(underlying: String)
case class Password(underlying: String)
case class User(email: Email, password: Password)
abstract class UserResponse
case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse
case class FailedToAuthenticate(msg: String) extends UserResponse
