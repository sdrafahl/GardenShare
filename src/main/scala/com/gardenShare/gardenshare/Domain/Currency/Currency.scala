package com.gardenShare.gardenshare

import io.circe.Decoder

sealed abstract class Currency
case object USD extends Currency

object Currency {
  implicit def createCurrencyDecoder(implicit parser: Parser[Currency]) = Decoder.decodeString.emap(f => parser.parse(f))

  implicit object CurrencyParser extends Parser[Currency] {
    def parse(x:String): Either[String, Currency] = x match {
      case "USD" => Right(USD)
      case _ => Left(s"Invalid currency provided: ${x}")
    }
  }

  implicit object CurrencyEncoder extends EncodeToString[Currency] {
    def encode(x:Currency): String = x match {
      case USD => "USD"
    }
  }
}
