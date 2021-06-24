package com.gardenShare.gardenshare

import cats.Show
import cats.kernel.Hash
import cats.kernel.Order
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Product(storeId: Int, productName: Produce, am: Amount)

object Product {
  implicit lazy final val productCodec: Codec[Product] = deriveCodec
}
