package com.gardenShare.gardenshare

import com.stripe.model.Account
import com.gardenShare.gardenshare.Email
import java.net.URI
import com.stripe.model.AccountLink

sealed abstract class PaymentCommand[A]
case class ChargeStripeToken() extends PaymentCommand[String]
case class CreateStripeConnectedAccount(sellerEmail:Email) extends PaymentCommand[Account]
case class CreateStripeAccountLink(sellerId: String, refreshUrl: URI, returnUrl: URI) extends PaymentCommand[AccountLink]
case class GetStripeAccountStatus(sellerStripeId: String) extends PaymentCommand[Account]
case class ClearStripeAccounts(emails: List[Email]) extends PaymentCommand[Unit]
