package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.Price.PriceOps

abstract class DeterminePaymentFee[F[_]] {
  def determineFee(amount: Amount, sellerEmail: Email): F[Amount]
}

object DeterminePaymentFee {
  implicit object DefaultIODeterminePaymentFee extends DeterminePaymentFee[IO] {
    def determineFee(amount: Amount, sellerEmail: Email): IO[Amount] = IO.pure(Amount(amount.quantityOfCurrency * .05, amount.currencyType))
  }
}
