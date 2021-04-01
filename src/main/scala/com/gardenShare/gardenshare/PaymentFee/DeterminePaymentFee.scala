package com.gardenShare.gardenshare

import cats.effect.IO


abstract class DeterminePaymentFee[F[_]] {
  def determineFee(amount: Amount, sellerEmail: Email): F[Amount]
}

object DeterminePaymentFee {
  implicit object DefaultIODeterminePaymentFee extends DeterminePaymentFee[IO] {
    def determineFee(amount: Amount, sellerEmail: Email): IO[Amount] = IO.pure(Amount((amount.quantityOfCurrency * .05).toInt, amount.currencyType))
  }
}
