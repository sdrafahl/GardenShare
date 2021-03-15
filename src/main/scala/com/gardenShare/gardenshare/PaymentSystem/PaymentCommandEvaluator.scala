package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.CreateStripeConnectedAccountEvaluator.CreateStripeConnectedAccountEvaluatorOps
import cats.effect.ContextShift
import com.gardenShare.gardenshare.ClearStripeAccountsEvaluator.ClearStripeAccountsEvaluatorOps

abstract class PaymentCommandEvaluator[F[_]] {
  def eval[A](c: PaymentCommand[A])(implicit cs: ContextShift[F]): F[A]
}

object PaymentCommandEvaluator {
  implicit def createIOPaymentCommandEvaluator(
    implicit createAccountEval: CreateStripeConnectedAccountEvaluator[IO],
    createLinkEval: CreateAccountLinkEvalutor[IO],
    getStatusEvaluator: GetStripeAccountStatusEvaluator[IO],
    clearStripeAccoutns: ClearStripeAccountsEvaluator[IO]
  ) = new PaymentCommandEvaluator[IO] {
    def eval[A](c: PaymentCommand[A])(
      implicit cs: ContextShift[IO]
    ) = {
      c match {
        case ChargeStripeToken() => ???
        case CreateStripeConnectedAccount(e) => createAccountEval.eval(CreateStripeConnectedAccount(e))
        case CreateStripeAccountLink(e, refreshurl, returnurl) => createLinkEval.eval(CreateStripeAccountLink(e, refreshurl, returnurl))
        case GetStripeAccountStatus(se) => getStatusEvaluator.eval(GetStripeAccountStatus(se))
        case ClearStripeAccounts(emails) => ClearStripeAccounts(emails).evaluate
        case _ => IO.raiseError(new Throwable("Not implemented"))
      }
    }
  }

  implicit class PaymentCommandEvaluatorOps[A](underlying: PaymentCommand[A]) {
    def evaluate[F[_]: PaymentCommandEvaluator: ContextShift] = implicitly[PaymentCommandEvaluator[F]].eval(underlying)
  }
}
