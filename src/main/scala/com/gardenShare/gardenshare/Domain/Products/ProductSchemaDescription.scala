package com.gardenShare.gardenshare

abstract class PriceUnit
case object Pound extends PriceUnit
case object Units extends PriceUnit
case object Invalid extends PriceUnit

sealed abstract class Produce
case object BrownOysterMushrooms extends Produce

case class ProductDescription(name: String, priceUnit: PriceUnit, product: Produce)
