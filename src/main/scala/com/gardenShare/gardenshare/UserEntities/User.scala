package com.gardenShare.gardenshare.UserEntities

case class Email(underlying: String)
case class Password(underlying: String)
case class User(email: Email, password: Password)
abstract class UserResponse
case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse
case class FailedToAuthenticate(msg: String) extends UserResponse
abstract class JWTValidationResult
case class InvalidToken(msg: String) extends JWTValidationResult

abstract class Group
case class Seller() extends Group

case class ValidToken(email: Option[String], groups: List[Group]) extends JWTValidationResult
case class JWTValidationTokens(idToken: String)


