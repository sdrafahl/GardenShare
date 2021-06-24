package com.gardenShare.gardenshare

sealed abstract class PriceUnit
case object Pound extends PriceUnit
case object Units extends PriceUnit
case object Invalid extends PriceUnit

object PriceUnit {

  def unapply(st: String): Option[PriceUnit] = st match {
    case "Pound" => Option(Pound)
    case "Units" => Option(Units)
    case _ => None
  }
  
  implicit object PriceUnitStringEncode extends EncodeToString[PriceUnit] {
    def encode(x:PriceUnit): String = x match {
      case Pound => "Pound"
      case Units => "Units"
      case Invalid => "Invalid"
    }
  }
}
