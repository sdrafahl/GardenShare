package com.gardenShare.gardenshare

import cats.effect.IO
import com.stripe.Stripe
import com.stripe.model.Account

abstract class DeleteAccountCommandEvaluator[F[_]] {
  def eval(c: DeleteAccountCommand): F[Unit]
}

object DeleteAccountCommandEvaluator {
  implicit def createIODeleteAccountCommandEvaluator(implicit getStripeKey: GetStripePrivateKey[IO]) = new DeleteAccountCommandEvaluator[IO] {
    def eval(c: DeleteAccountCommand): IO[Unit] = {
      for {
        stripeApiKey <- getStripeKey.getKey
        _ <- IO(Stripe.apiKey = stripeApiKey.n)
        account <- IO(Account.retrieve(c.accountId))
        _ <- IO(account.delete())
      } yield ()
    }
  }
  implicit class DeleteAccountCommandEvaluatorOps(underlying: DeleteAccountCommand) {
    def evaluate[F[_]: DeleteAccountCommandEvaluator] = implicitly[DeleteAccountCommandEvaluator[F]].eval(underlying)
  }
}
