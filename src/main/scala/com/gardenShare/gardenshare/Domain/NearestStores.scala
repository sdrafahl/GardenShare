package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class NearestStores(store: List[RelativeDistanceAndStore])

object NearestStores {
  implicit lazy final val NearestStoresCodec: Codec[NearestStores] = deriveCodec
}
