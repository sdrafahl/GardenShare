package com.gardenShare.gardenshare

import io.circe._

sealed abstract class Currency
case object USD extends Currency

object Currency {
  def unapply(st: String): Option[Currency] = st match {
    case "USD" => Option(USD)
    case _ => None
  }

  private lazy val currencyDecoder: Decoder[Currency] = Decoder.decodeString.emap(a => unapply(a).fold[Either[String, Currency]](Left("Invalid currency"))(aa => Right(aa)))
  private lazy val currencyEncoder: Encoder[Currency] = Encoder.encodeString.contramap[Currency] {
    case USD => "USD"
  }

  implicit lazy val currencyCodec: Codec[Currency] = Codec.from(currencyDecoder, currencyEncoder)

  implicit def createCurrencyDecoder(implicit parser: Parser[Currency]) = Decoder.decodeString.emap(f => parser.parse(f))    
}
