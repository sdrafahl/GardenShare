package com.gardenShare.gardenshare

case class JWTValidationTokens(idToken: String)

object JWTValidationTokens {
  def unapply(str: String): Option[JWTValidationTokens] = Some(JWTValidationTokens(str))
}
