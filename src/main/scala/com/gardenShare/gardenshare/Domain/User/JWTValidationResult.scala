package com.gardenShare.gardenshare

abstract class JWTValidationResult
case class InvalidToken(msg: String) extends JWTValidationResult
case class ValidToken(email: Option[Email]) extends JWTValidationResult
