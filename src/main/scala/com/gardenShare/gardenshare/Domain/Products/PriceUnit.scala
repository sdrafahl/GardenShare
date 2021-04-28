package com.gardenShare.gardenshare

sealed abstract class PriceUnit
case object Pound extends PriceUnit
case object Units extends PriceUnit
case object Invalid extends PriceUnit

object PriceUnit {
  implicit object PriceUnitParser extends Parser[PriceUnit] {
    def parse(x:String): Either[String, PriceUnit] = x match {
      case "Pound" => Right(Pound)
      case "Units" => Right(Units)
      case _ => Left("Invalid price unit")
    }
  }

  implicit object PriceUnitStringEncode extends EncodeToString[PriceUnit] {
    def encode(x:PriceUnit): String = x match {
      case Pound => "Pound"
      case Units => "Units"
      case Invalid => "Invalid"
    }
  }
}
