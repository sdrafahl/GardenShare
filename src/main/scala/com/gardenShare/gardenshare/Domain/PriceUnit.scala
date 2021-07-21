package com.gardenShare.gardenshare

import io.circe._

sealed abstract class PriceUnit

object PriceUnit {

  case object Pound extends PriceUnit
  case object Units extends PriceUnit
  case object Invalid extends PriceUnit

  def unapply(st: String): Option[PriceUnit] = st match {
    case "Pound" => Option(Pound)
    case "Units" => Option(Units)
    case _ => None
  }

  private[this] def parsePriceUnit(s: String): Either[String, PriceUnit] = unapply(s) match {
    case None    => Left("Invalid Price Unit")
    case Some(a) => Right(a)
  }

  private[this] lazy val priceUnitEncoder: Encoder[PriceUnit] = Encoder.encodeString.contramap[PriceUnit]{
    case Pound => "Pound"
    case Units => "Units"
    case Invalid => "Invalid"
  }

  private[this] lazy val priceUnitDecoder: Decoder[PriceUnit] = Decoder.decodeString.emap(parsePriceUnit)

  implicit lazy val priceUnitCodec: Codec[PriceUnit] = Codec.from(priceUnitDecoder, priceUnitEncoder)
  
}
