package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class PaymentIntentToken(token: String) extends AnyVal

object PaymentIntentToken {
  implicit lazy final val paymentIntentTokenCodec: Codec[PaymentIntentToken] = deriveCodec
}
