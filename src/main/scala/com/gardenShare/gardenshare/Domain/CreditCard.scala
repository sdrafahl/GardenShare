package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class CreditCard(number: String, monthNumber: Int, expYear: Int, cvc: Int)

object CreditCard {
  implicit lazy final val creditCardCodec: Codec[CreditCard] = deriveCodec
}
