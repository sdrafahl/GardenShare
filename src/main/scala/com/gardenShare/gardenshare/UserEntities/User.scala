package com.gardenShare.gardenshare.UserEntities

case class Email(underlying: String)
case class Password(underlying: String)
case class User(email: Email, password: Password)
case class UserAndType(usr: User, userType: UserType)
abstract class UserResponse
case class AuthenticatedUser(user: User, jwt: String, accessToken: String) extends UserResponse
case class FailedToAuthenticate(msg: String) extends UserResponse
abstract class JWTValidationResult
case class InvalidToken(msg: String) extends JWTValidationResult

case class ValidToken(email: Option[String]) extends JWTValidationResult
case class JWTValidationTokens(idToken: String)

abstract class UserType
object Requester extends UserType
object Sellers extends UserType
case object InvalidType extends UserType

abstract class CreateWorkerResponse
case class WorkerCreatedSuccessfully() extends CreateWorkerResponse
case class WorkerFailedToCreate(msg: String) extends CreateWorkerResponse 

