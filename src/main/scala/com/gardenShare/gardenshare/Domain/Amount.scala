package com.gardenShare.gardenshare

import cats.Monoid
import cats.Show
import cats.kernel.Hash
import cats.kernel.Order
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import com.gardenShare.gardenshare.Currency.USD

case class Amount(quantityOfCurrency: Price, currencyType: Currency)

object Amount {
  implicit object MonoidGroupAmount extends Monoid[Amount] {
    def empty = Amount(Price(0), USD)
    def combine(x: Amount, y: Amount) = (x.currencyType, y.currencyType) match {
      case (USD, USD) => Amount(Price(x.quantityOfCurrency.value + y.quantityOfCurrency.value), USD)
    }
  }

  implicit lazy final val amountCodec: Codec[Amount] = deriveCodec
}
