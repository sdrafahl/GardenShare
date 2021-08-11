package com.gardenShare.gardenshare

import cats.effect.IO
import cats.effect.ContextShift
import com.stripe.model.PaymentIntent
import com.stripe.Stripe
import com.stripe.model.PaymentMethod
import com.stripe.param.PaymentIntentConfirmParams
import com.stripe.param.PaymentMethodCreateParams
import com.stripe.param.PaymentMethodCreateParams.CardDetails

abstract class ConfirmPaymentIntentEvaluator[F[_]] {
  def eval(c: ConfirmPaymentIntentCard)(implicit cs: ContextShift[F]): F[Unit]
}

object ConfirmPaymentIntentEvaluator {
  implicit def createIOConfirmPaymentIntentEvaluator(implicit getStripeKey: GetStripePrivateKey[IO]) = new ConfirmPaymentIntentEvaluator[IO] {
    def eval(c: ConfirmPaymentIntentCard)(implicit cs: ContextShift[IO]): IO[Unit] = {      
      for {
        stripeApiKey <- getStripeKey.getKey        
        _ <- IO(Stripe.apiKey = stripeApiKey.n)
        intent <- IO(PaymentIntent.retrieve(c.paymentIntentID))
        cardDetails = CardDetails
        .builder()
        .setNumber(c.card.number)
        .setExpYear(c.card.expYear.toLong)
        .setExpMonth(c.card.monthNumber.toLong)
        .setCvc(c.card.cvc.toString())        
        .build()

        methodParams = PaymentMethodCreateParams
        .builder()
        .setCard(cardDetails)
        .setType(PaymentMethodCreateParams.Type.CARD)
        .build()

        paymentMethod <- IO(PaymentMethod.create(methodParams))

        paymentIntentConfirmParams = PaymentIntentConfirmParams
        .builder()
        .setPaymentMethod(paymentMethod.getId())
        .build()        
        
        _ <- IO(intent.confirm(paymentIntentConfirmParams))        
      } yield ()
    }
  }

  implicit class ConfirmPaymentIntentEvaluatorOps(underlying: ConfirmPaymentIntentCard) {
    def evaluate[F[_]: ConfirmPaymentIntentEvaluator:ContextShift] = implicitly[ConfirmPaymentIntentEvaluator[F]].eval(underlying)
  }
}
