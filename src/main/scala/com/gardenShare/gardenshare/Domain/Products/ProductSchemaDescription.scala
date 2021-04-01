package com.gardenShare.gardenshare

import io.circe.{ Decoder, Encoder}

abstract class PriceUnit
case object Pound extends PriceUnit
case object Units extends PriceUnit
case object Invalid extends PriceUnit

sealed abstract class Produce
case object BrownOysterMushrooms extends Produce
case class ProductDescription(name: String, priceUnit: PriceUnit, product: Produce)


object Produce {
  implicit def createProduceDecoder(implicit pp: ParseProduce[String]) = Decoder.decodeString.emap{(s: String) => pp.parse(s)}
}
