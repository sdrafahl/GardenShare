package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.PaymentCommandEvaluator.PaymentCommandEvaluatorOps
import cats.effect.ContextShift

case class PaymentIntentToken(token: String)

abstract class InitiatePayment[F[_]] {
  def initiatePayment(amount: Amount, receiptEmail: Email, sellerEmail: Email, paymentType: PaymentType)(implicit cs: ContextShift[IO]): F[PaymentIntentToken]
}

object InitiatePayment {
  implicit def createIOInitiatePayment(implicit evaluator: PaymentCommandEvaluator[IO], determinePayment: DeterminePaymentFee[IO], translator: TranslateAmountIntoRealAmount, searchForId: SearchAccountIdsByEmail[IO]) = new InitiatePayment[IO] {
    def initiatePayment(amount: Amount, receiptEmail: Email, sellerEmail: Email, paymentType: PaymentType)(implicit cs: ContextShift[IO]): IO[PaymentIntentToken] = {
      for {
        fee <- determinePayment.determineFee(amount, sellerEmail)
        maybeStripeID <- searchForId.search(sellerEmail)
        intent <- maybeStripeID match {
          case None => IO.raiseError(new Throwable(s"There is no Stripe account reference for ${sellerEmail}"))
          case Some(e) => for {
            intent <- CreatePaymentIntentEvaluatorCommand(
              translator.translate(amount),
              amount.currencyType,
              translator.translate(fee),
              paymentType,
              receiptEmail,
              e
            ).evaluate
          } yield intent
        }
      } yield PaymentIntentToken(intent.getClientSecret())
    }
  }
}
