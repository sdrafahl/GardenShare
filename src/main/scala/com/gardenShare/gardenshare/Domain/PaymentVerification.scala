package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class PaymentVerification(status: PaymentVerificationStatus)

object PaymentVerification {
  implicit lazy final val paymentVerificationCodec: Codec[PaymentVerification] = deriveCodec
}
