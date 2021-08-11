package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class RelativeDistanceAndStore(store: Store, distance: DistanceInMiles)

object RelativeDistanceAndStore {
  implicit lazy final val relativeDistanceAndStoreCodec: Codec[RelativeDistanceAndStore] = deriveCodec
}
