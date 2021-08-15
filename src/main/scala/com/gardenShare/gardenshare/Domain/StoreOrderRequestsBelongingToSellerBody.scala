package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class StoreOrderRequestsBelongingToSellerBody(body: List[StoreOrderRequestWithId])

object StoreOrderRequestsBelongingToSellerBody {
  implicit lazy final val storeOrderRequestsBelongingToSellerBodyCodec: Codec[StoreOrderRequestsBelongingToSellerBody] = deriveCodec
}
