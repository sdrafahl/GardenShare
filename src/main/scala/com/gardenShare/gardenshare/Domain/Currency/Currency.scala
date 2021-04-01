package com.gardenShare.gardenshare

import cats.Monoid
import io.circe.{ Decoder, Encoder}

sealed abstract class Currency
case object USD extends Currency

case class Amount(quantityOfCurrency: Int, currencyType: Currency)

package object AmountOps {
  implicit object MonoidGroupAmount extends Monoid[Amount] {
    def empty = Amount(0, USD)
    def combine(x: Amount, y: Amount) = (x.currencyType, y.currencyType) match {
      case (USD, USD) => Amount(x.quantityOfCurrency + y.quantityOfCurrency, USD)
    }
  }
}

object Currency {
  implicit def createCurrencyDecoder(implicit parser: Parser[Currency]) = Decoder.decodeString.emap(f => parser.parse(f))
}
