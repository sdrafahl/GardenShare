package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class ListOfStores(l: List[Store])

object ListOfStores {
  implicit lazy final val ListOfStoresCodec: Codec[ListOfStores] = deriveCodec
}
