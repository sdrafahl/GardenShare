package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
import slick.lifted.Isomorphism

case class ProductId(id: Int) extends AnyVal

object ProductId {
  def unapply(str: String): Option[ProductId] = str.toIntOption.map(ProductId(_))

  implicit lazy final val productIdCodec: Codec[ProductId] = deriveCodec

  implicit lazy final val intIsomorphism = new Isomorphism[ProductId, Int]((o: ProductId) => o.id, (i: Int) => ProductId(i))
}
