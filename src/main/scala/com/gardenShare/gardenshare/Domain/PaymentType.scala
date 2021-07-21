package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Parser
import io.circe._

sealed abstract class PaymentType

object PaymentType {

  case object Card extends PaymentType

  private[this] def parse(x:String): Either[String, PaymentType] = x match {
    case "card" => Right(Card)
    case _ => Left(s"Failed to parse into payment type")
  }


  private[this] lazy val paymentDecoder: Decoder[PaymentType] = Decoder.decodeString.emap(parse)

  private[this] def encode(x: PaymentType) = x match {
    case Card => "card"
  }

  private[this] lazy val paymentEncoder: Encoder[PaymentType] = Encoder.encodeString.contramap[PaymentType](encode)

  implicit lazy val paymentTypeCodec: Codec[PaymentType] = Codec.from(paymentDecoder, paymentEncoder)
  
}
