package com.gardenShare.gardenshare

abstract class GetproductDescription[T] {
  def gestDesc(c: T): ProductDescription
}

object GetproductDescription {
  implicit object DefaultGetproductDescription extends GetproductDescription[Produce] {
    def gestDesc(c: Produce): ProductDescription = c match {
      case BrownOysterMushrooms => ProductDescription("Brown-Oyster-Mushrooms", Pound, c)
    }
  }
  implicit class GetproductDescriptionOps[T](underlying: T) {
    def getProductDescription(implicit a:GetproductDescription[T]) = a.gestDesc(underlying)
  }
}



