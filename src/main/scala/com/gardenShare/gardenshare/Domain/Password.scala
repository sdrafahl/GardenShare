package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.extras.semiauto._

final case class Password(underlying: String) extends AnyVal

object Password {
  def unapply(string: String) = Some(Password(string))
  implicit lazy final val passwordCodec: Codec[Password] = deriveUnwrappedCodec
}
