package com.gardenShare.gardenshare

import java.net.URL
import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec
import EncodersDecoders._

case class ApplyUserToBecomeSellerResponse(url: URL)

object ApplyUserToBecomeSellerResponse {
  implicit lazy final val codecApplyUserToBecomeSellerResponse: Codec[ApplyUserToBecomeSellerResponse] = deriveCodec
}
