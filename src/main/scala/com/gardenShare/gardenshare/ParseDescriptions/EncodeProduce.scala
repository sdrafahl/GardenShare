package com.gardenShare.gardenshare

abstract class EncodeProduce[T] {
  def to(p:Produce): T
}

object EncodeProduce {
  implicit object StringEncodeProduce extends EncodeProduce[String] {
    def to(p:Produce): String = p match {
      case BrownOysterMushrooms => "BrownOysterMushrooms"
    }
  }
}
