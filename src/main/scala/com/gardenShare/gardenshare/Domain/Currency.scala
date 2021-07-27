package com.gardenShare.gardenshare

import io.circe._
import cats.Show
import io.circe.Codec

sealed abstract class Currency

object Currency {

  case object USD extends Currency

  def unapply(str: String): Option[Currency] = decoderCurrency(str).toOption

  def encodeCurrency(c: Currency) = c match {
    case USD => "USD"
  }

  def decoderCurrency(str: String): Either[String, Currency] = str match {
    case "USD" => Right(USD)
    case x => Left(s"Invalid currency provided: ${x}")
  }

  private lazy val currencyDecoder: Decoder[Currency] = Decoder.decodeString.emap(decoderCurrency)

  private lazy val currencyEncoder: Encoder[Currency] = Encoder.encodeString.contramap[Currency](encodeCurrency)

  implicit lazy val currencyCodec: Codec[Currency] = Codec.from(currencyDecoder, currencyEncoder)

  implicit lazy val CurrencyShow: Show[Currency] = new Show[Currency] {
    def show(c: Currency) = encodeCurrency(c)
  }
    
}

