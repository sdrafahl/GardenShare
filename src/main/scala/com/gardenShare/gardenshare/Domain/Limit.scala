package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Limit(l: Int) extends AnyVal

object Limit {
  def unapply(str: String): Option[Limit] = str.toIntOption.map(Limit(_))

  implicit lazy final val limitCodec: Codec[Limit] = deriveCodec
}
