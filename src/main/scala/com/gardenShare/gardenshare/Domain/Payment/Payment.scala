package com.gardenShare.gardenshare.domain.Payment

case class PaymentCard(cardNumber: String, expMonth: Int, expYear: Int, cvc: String)
case class Payment(c: PaymentCard, amount: BigDecimal)
