package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
import slick.lifted.Isomorphism

case class StoreRequestId(id: Int) extends AnyVal

object StoreRequestId {
  def unapply(str: String): Option[StoreRequestId] = str.toIntOption.map(StoreRequestId(_))

  implicit lazy final val storeRequestIdCodec: Codec[StoreRequestId] = deriveCodec

  implicit lazy final val intIsomorphism = new Isomorphism[StoreRequestId, Int]((o: StoreRequestId) => o.id, (i: Int) => StoreRequestId(i))
}
