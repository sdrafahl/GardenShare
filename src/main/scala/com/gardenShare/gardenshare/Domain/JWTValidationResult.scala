package com.gardenShare.gardenshare

sealed abstract class JWTValidationResult

object JWTValidationResult {
  case class InvalidToken(msg: String) extends JWTValidationResult
  case class ValidToken(email: Option[Email]) extends JWTValidationResult
}
