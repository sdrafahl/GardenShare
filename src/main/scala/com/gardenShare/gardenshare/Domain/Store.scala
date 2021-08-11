package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class Store(id: Int, address: Address, sellerEmail: Email)

object Store {
  implicit lazy final val storeCodec: Codec[Store] = deriveCodec
}
