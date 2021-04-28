package com.gardenShare.gardenshare


sealed abstract class Produce
case object BrownOysterMushrooms extends Produce

object Produce {
  implicit object ProduceParser extends Parser[Produce] {
    def parse(x:String): Either[String, Produce] = x match {
      case "BrownOysterMushrooms" => Right(BrownOysterMushrooms)
      case s => Left(s"Invalid produce given ${s} is not a valid produce")
    }
  }

  implicit object ProduceEncodeToString extends EncodeToString[Produce] {
    def encode(x:Produce): String = x match {
      case BrownOysterMushrooms => "BrownOysterMushrooms"
    }
  }
}

