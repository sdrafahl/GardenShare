package com.gardenShare.gardenshare

import java.net.URL
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import com.gardenShare.gardenshare.EncodersDecoders._

case class ApplyUserToBecomeSellerData(address: Address, refreshUrl: URL, returnUrl: URL)

object ApplyUserToBecomeSellerData {
  implicit lazy final val applyUserToBecomeSellerDataCodec: Codec[ApplyUserToBecomeSellerData] = deriveCodec
}
