package com.gardenShare.gardenshare

import cats.Monoid


case class Amount(quantityOfCurrency: Int, currencyType: Currency)

object Amount {
  implicit object MonoidGroupAmount extends Monoid[Amount] {
    def empty = Amount(0, USD)
    def combine(x: Amount, y: Amount) = (x.currencyType, y.currencyType) match {
      case (USD, USD) => Amount(x.quantityOfCurrency + y.quantityOfCurrency, USD)
    }
  }
}
