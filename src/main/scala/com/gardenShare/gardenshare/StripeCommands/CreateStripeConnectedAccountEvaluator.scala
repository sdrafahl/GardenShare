package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.GetStripePrivateKey
import com.stripe.Stripe
import com.stripe.param.AccountCreateParams
import com.stripe.model.Account

abstract class CreateStripeConnectedAccountEvaluator[F[_]] {
  def eval(x: CreateStripeConnectedAccount): F[Account]
}

object CreateStripeConnectedAccountEvaluator {
  implicit def createIOCreateStripeConnectedAccountEvaluator(implicit getStripeKey: GetStripePrivateKey[IO]) = new CreateStripeConnectedAccountEvaluator[IO] {
    def eval(x: CreateStripeConnectedAccount): IO[Account] = {
      for {
        stripeApiKey <- getStripeKey.getKey
        _ <- IO(Stripe.apiKey = stripeApiKey.n)
        accountCreateParamsCommand = AccountCreateParams
          .builder()
          .setType(AccountCreateParams.Type.EXPRESS)
          .setEmail(x.sellerEmail.underlying.value) 
          .build()
        responseFromCreatingAccount <- IO(Account.create(accountCreateParamsCommand))
      } yield responseFromCreatingAccount
    }
  }

  implicit class CreateStripeConnectedAccountEvaluatorOps(underlying:CreateStripeConnectedAccount) {
    def evaluate[F[_]: CreateStripeConnectedAccountEvaluator] = implicitly[CreateStripeConnectedAccountEvaluator[F]].eval(underlying)
  }
}
