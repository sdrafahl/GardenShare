package com.gardenShare.gardenshare

sealed abstract class Currency
case object USD extends Currency

case class Amount(quantityOfCurrency: Int, currencyType: Currency)
