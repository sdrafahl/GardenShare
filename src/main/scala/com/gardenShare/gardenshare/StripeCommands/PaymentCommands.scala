package com.gardenShare.gardenshare

import com.stripe.model.Account
import com.gardenShare.gardenshare.Email
import java.net.URL
import com.stripe.model.AccountLink
import com.stripe.model.PaymentIntent

sealed abstract class PaymentCommand[A]
case class CreateStripeConnectedAccount(sellerEmail:Email) extends PaymentCommand[Account]
case class CreateStripeAccountLink(sellerId: String, refreshUrl: URL, returnUrl: URL) extends PaymentCommand[AccountLink]
case class GetStripeAccountStatus(sellerStripeId: String) extends PaymentCommand[Account]
case class ClearStripeAccounts(emails: List[Email]) extends PaymentCommand[Unit]
case class CreatePaymentIntentEvaluatorCommand(amount: Long, currency: Currency, fee: Long, paymentType: PaymentType, recieptEmail: Email, sellerConnectedAccountId: String) extends PaymentCommand[PaymentIntent]
case class GetPaymentIntentCommand(intentId: String) extends PaymentCommand[PaymentIntent]
case class CreateCustomAccountCommand() extends PaymentCommand[Account]
case class DeleteAccountCommand(accountId: String) extends PaymentCommand[Unit]
case class ConfirmPaymentIntentCard(paymentIntentID: String, card: CreditCard) extends PaymentCommand[Unit]

