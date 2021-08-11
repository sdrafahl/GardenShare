package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class JWTValidationTokens(idToken: String)

object JWTValidationTokens {
  def unapply(str: String): Option[JWTValidationTokens] = Some(JWTValidationTokens(str))

  implicit lazy final val JWTValidationTokensCodec: Codec[JWTValidationTokens] = deriveCodec
}
