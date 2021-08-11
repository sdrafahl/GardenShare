package com.gardenShare.gardenshare

import cats.effect.IO
import cats.effect.ContextShift
import com.gardenShare.gardenshare.ClearStripeAccountsEvaluator.ClearStripeAccountsEvaluatorOps
import com.gardenShare.gardenshare.CreatePaymentIntentEvaluator.CreatePaymentIntentEvaluatorOps
import com.gardenShare.gardenshare.GetPaymentIntentEvaluator.GetPaymentIntentEvaluatorOps
import com.gardenShare.gardenshare.CreateCustomAccountEvaluator.CreateCustomAccountEvaluatorOps
import com.gardenShare.gardenshare.DeleteAccountCommandEvaluator.DeleteAccountCommandEvaluatorOps
import ConfirmPaymentIntentEvaluator.ConfirmPaymentIntentEvaluatorOps

abstract class PaymentCommandEvaluator[F[_]] {
  def eval[A](c: PaymentCommand[A])(implicit cs: ContextShift[F]): F[A]
}

object PaymentCommandEvaluator {
  implicit def createIOPaymentCommandEvaluator(
    implicit createAccountEval: CreateStripeConnectedAccountEvaluator[IO],
    createLinkEval: CreateAccountLinkEvalutor[IO],
    getStatusEvaluator: GetStripeAccountStatusEvaluator[IO],
    clearStripeAccoutns: ClearStripeAccountsEvaluator[IO],
    createPaymentIntentEvaluator: CreatePaymentIntentEvaluator[IO],
    getPaymentIntentEvaluator: GetPaymentIntentEvaluator[IO],
    createCustomAccountEvaluator: CreateCustomAccountEvaluator[IO],
    deleteAccountCommandEvaluator: DeleteAccountCommandEvaluator[IO],
    confirmPaymentIntentEvaluator: ConfirmPaymentIntentEvaluator[IO]
  ) = new PaymentCommandEvaluator[IO] {
    def eval[A](c: PaymentCommand[A])(
      implicit cs: ContextShift[IO]
    ) = {
      c match {
        case CreateStripeConnectedAccount(e) => createAccountEval.eval(CreateStripeConnectedAccount(e))
        case CreateStripeAccountLink(e, refreshurl, returnurl) => createLinkEval.eval(CreateStripeAccountLink(e, refreshurl, returnurl))
        case GetStripeAccountStatus(se) => getStatusEvaluator.eval(GetStripeAccountStatus(se))
        case ClearStripeAccounts(emails) => ClearStripeAccounts(emails).evaluate
        case CreatePaymentIntentEvaluatorCommand(a, b, c, d, e, f) => CreatePaymentIntentEvaluatorCommand(a, b, c, d, e, f).evaluate
        case GetPaymentIntentCommand(id) => GetPaymentIntentCommand(id).evaluate
        case CreateCustomAccountCommand() => CreateCustomAccountCommand().evaluate
        case DeleteAccountCommand(id) => DeleteAccountCommand(id).evaluate
        case ConfirmPaymentIntentCard(id, card) => ConfirmPaymentIntentCard(id, card).evaluate
        case _ => IO.raiseError(new Throwable("Not implemented"))
      }
    }
  }

  implicit class PaymentCommandEvaluatorOps[A](underlying: PaymentCommand[A]) {
    def evaluate[F[_]: PaymentCommandEvaluator: ContextShift] = implicitly[PaymentCommandEvaluator[F]].eval(underlying)
  }
}
