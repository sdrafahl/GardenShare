package com.gardenShare.gardenshare.domain.Products

abstract class PriceUnit
case object Pound extends PriceUnit
case object Units extends PriceUnit
case object Invalid extends PriceUnit

case class ProductDescription(name: String, priceUnit: PriceUnit)
