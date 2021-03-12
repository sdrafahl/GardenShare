package com.gardenShare.gardenshare

import com.stripe.model.Account
import com.gardenShare.gardenshare.Email
import java.net.URL
import com.stripe.model.AccountLink

sealed abstract class PaymentCommand[A]
case class ChargeStripeToken() extends PaymentCommand[String]
case class CreateStripeConnectedAccount(sellerEmail:Email) extends PaymentCommand[Account]
case class CreateStripeAccountLink(sellerEmail: Email, refreshUrl: URL, returnUrl: URL) extends PaymentCommand[AccountLink]
case class GetStripeAccountStatus(sellerEmail: Email) extends PaymentCommand[Account]

