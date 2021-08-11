package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class OrderId(id: Int) extends AnyVal

object OrderId {
  def unapply(str: String): Option[OrderId] = str.toIntOption.map(OrderId(_))

  implicit lazy final val orderIdCodec: Codec[OrderId] = deriveCodec
}
