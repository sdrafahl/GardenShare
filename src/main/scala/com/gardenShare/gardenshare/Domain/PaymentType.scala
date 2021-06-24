package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Parser

sealed abstract class PaymentType
case object Card extends PaymentType

object PaymentType {
  implicit object PaymentTypeParser extends Parser[PaymentType] {
    def parse(x:String): Either[String, PaymentType] = x match {
        case "card" => Right(Card)
        case _ => Left(s"Failed to parse into payment type")
      }
  }

  implicit object PaymentTypeEncoder extends EncodeToString[PaymentType] {
    def encode(x: PaymentType) = x match {
      case Card => "card"
    }
  }
}
