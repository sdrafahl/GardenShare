package com.gardenShare.gardenshare

abstract class ParseCurrency {
  def parse(s: String): Either[Throwable ,Currency]
}

object ParseCurrency {
  def apply(implicit x: ParseCurrency) = x
  implicit object DefaultParseCurrency extends ParseCurrency {
    def parse(s: String): Either[Throwable ,Currency] = {
      s match {
        case "USD" => Right(USD)
        case _ => Left(new Throwable(s"Failed to parse ${s} into currency"))
      }
    }
  }
}

abstract class DecodeCurrency {
  def decode(s: Currency): String
}

object DecodeCurrency {
  def apply(implicit x: DecodeCurrency) = x

  implicit object DefaultDecodeCurrency extends DecodeCurrency {
    def decode(s: Currency): String = s match {
      case USD => "USD"
    }
  }
}
