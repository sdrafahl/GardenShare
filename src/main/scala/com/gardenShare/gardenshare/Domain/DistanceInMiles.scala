package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class DistanceInMiles(distance: Double)

object DistanceInMiles {

  def unapply(str: String): Option[DistanceInMiles] = str.toDoubleOption.map(DistanceInMiles(_))
  implicit lazy final val distanceInMilesCodec: Codec[DistanceInMiles] = deriveCodec
}
