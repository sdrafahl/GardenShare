package com.gardenShare.gardenshare

case class PaymentID(secretID: String)

object PaymentID {
  implicit class PaymentIDOps(underlying: PaymentID) {
    def parsePublicKey = {
      val parts = underlying.secretID.split("_")
      parts(0) + "_" + parts(1)
    }
  }
}
