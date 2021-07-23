package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class DistanceInMiles(distance: Double)

object DistanceInMiles {
  implicit lazy final val distanceInMilesCodec: Codec[DistanceInMiles] = deriveCodec
}
