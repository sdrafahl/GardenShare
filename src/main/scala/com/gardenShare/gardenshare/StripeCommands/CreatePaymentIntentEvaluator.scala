package com.gardenShare.gardenshare

import cats.effect.IO
import com.stripe.Stripe
import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams

abstract class CreatePaymentIntentEvaluator[F[_]] {
  def eval(c: CreatePaymentIntentEvaluatorCommand): F[PaymentIntent]
}

object CreatePaymentIntentEvaluator {
  def apply[F[_]: CreatePaymentIntentEvaluator]() = implicitly[CreatePaymentIntentEvaluator[F]]

  implicit def createIOCreatePaymentIntentEvaluator(implicit getStripeKey: GetStripePrivateKey[IO], decodePaymentType: EncodeToString[PaymentType]) = new CreatePaymentIntentEvaluator[IO] {
    def eval(c: CreatePaymentIntentEvaluatorCommand): IO[PaymentIntent] = {      
      for {
        stripeApiKey <- getStripeKey.getKey
        _ <- IO(Stripe.apiKey = stripeApiKey.n)
        curr = Currency.encodeCurrency(c.currency)
        paymentType = decodePaymentType.encode(c.paymentType)
        transerData = PaymentIntentCreateParams.TransferData
        .builder()
        .setDestination(c.sellerConnectedAccountId)
        .build()
        intentRequest = PaymentIntentCreateParams
        .builder()
        .setAmount(c.amount)
        .setReceiptEmail(c.recieptEmail.underlying.value)
        .setCurrency(curr)
        .addPaymentMethodType(paymentType)        
        .setApplicationFeeAmount(c.fee)
        .setTransferData(transerData)                
        .build()
        intent <- IO(PaymentIntent.create(intentRequest))
      } yield intent
    }
  }

  implicit class CreatePaymentIntentEvaluatorOps(underlying: CreatePaymentIntentEvaluatorCommand) {
    def evaluate[F[_]: CreatePaymentIntentEvaluator] = implicitly[CreatePaymentIntentEvaluator[F]].eval(underlying)
  }
}
