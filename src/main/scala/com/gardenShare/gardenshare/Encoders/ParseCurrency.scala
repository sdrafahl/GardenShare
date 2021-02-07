package com.gardenShare.gardenshare

abstract class Parser[A] {
  def parse(x:String): Either[String, A]
}

object Parser {
  implicit object CurrencyParser extends Parser[Currency] {
    def parse(x:String): Either[String, Currency] = x match {
      case "USD" => Right(USD)
      case _ => Left(s"Invalid currency provided: ${x}")
    }
  }
}

abstract class EncodeToString[A] {
  def encode(x:A): String
}

object EncodeToString {
  implicit object CurrencyEncoder extends EncodeToString[Currency] {
    def encode(x:Currency): String = x match {
      case USD => "USD"
    }
  }
}
