package com.gardenShare.gardenshare

import cats.effect.IO
import com.stripe.model.PaymentIntent
import com.stripe.Stripe

abstract class GetPaymentIntentEvaluator[F[_]] {
  def evaluate(c: GetPaymentIntentCommand): F[PaymentIntent]
}

object GetPaymentIntentEvaluator {
  implicit def createIOGetPaymentIntentEvaluator(implicit getStripeKey: GetStripePrivateKey[IO]) = new GetPaymentIntentEvaluator[IO] {
    def evaluate(c: GetPaymentIntentCommand): IO[PaymentIntent] = for {
      stripeApiKey <- getStripeKey.getKey
      _ <- IO(Stripe.apiKey = stripeApiKey.n)
      intent <- IO(PaymentIntent.retrieve((c.intentId)))
    } yield intent
  }

  implicit class GetPaymentIntentEvaluatorOps(underlying: GetPaymentIntentCommand) {
    def evaluate[F[_]: GetPaymentIntentEvaluator] = implicitly[GetPaymentIntentEvaluator[F]].evaluate(underlying)
  }
}
