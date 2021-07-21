package com.gardenShare.gardenshare

import scala.util.Try
import io.circe.Codec
import io.circe.generic.extras.semiauto._

case class PaymentID(secretID: String) extends AnyVal

object PaymentID {
  
  implicit lazy final val paymentIDCodec: Codec[PaymentID] = deriveUnwrappedCodec

  implicit class PaymentIDOps(underlying: PaymentID) {
    def parsePublicKey = {
      val parts = underlying.secretID.split("_")
      Try(parts(0) + "_" + parts(1))
    }
  }
}
