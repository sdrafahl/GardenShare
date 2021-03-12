package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.CreateStripeConnectedAccountEvaluator.CreateStripeConnectedAccountEvaluatorOps

abstract class PaymentCommandEvaluator[F[_]] {
  def eval[A](c: PaymentCommand[A]): F[A]
}

object PaymentCommandEvaluator {
  implicit def createIOPaymentCommandEvaluator(
    implicit createAccountEval: CreateStripeConnectedAccountEvaluator[IO],
    createLinkEval: CreateAccountLinkEvalutor[IO],
    getStatusEvaluator: GetStripeAccountStatusEvaluator[IO]
  ) = new PaymentCommandEvaluator[IO] {
    def eval[A](c: PaymentCommand[A]) = {
      c match {
        case ChargeStripeToken() => ???
        case CreateStripeConnectedAccount(e) => createAccountEval.eval(CreateStripeConnectedAccount(e))
        case CreateStripeAccountLink(e, refreshurl, returnurl) => createLinkEval.eval(CreateStripeAccountLink(e, refreshurl, returnurl))
        case GetStripeAccountStatus(se) => getStatusEvaluator.eval(GetStripeAccountStatus(se))
      }
    }
  }

  implicit class PaymentCommandEvaluatorOps[A](underlying: PaymentCommand[A]) {
    def evaluate[F[_]: PaymentCommandEvaluator] = implicitly[PaymentCommandEvaluator[F]].eval(underlying)
  }
}
