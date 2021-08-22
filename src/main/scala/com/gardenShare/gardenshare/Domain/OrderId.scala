package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
import slick.lifted.Isomorphism

case class OrderId(id: Int) extends AnyVal

object OrderId {
  def unapply(str: String): Option[OrderId] = str.toIntOption.map(OrderId(_))

  implicit lazy final val orderIdCodec: Codec[OrderId] = deriveCodec

  implicit lazy final val intIsomorphism = new Isomorphism[OrderId, Int]((o: OrderId) => o.id, (i: Int) => OrderId(i))
}
