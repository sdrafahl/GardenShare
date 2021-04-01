package com.gardenShare.gardenshare

import cats.effect.IO
import com.stripe.Stripe
import com.stripe.param.AccountCreateParams
import com.stripe.model.Account

abstract class CreateCustomAccountEvaluator[F[_]] {
  def eval(c: CreateCustomAccountCommand): F[Account]
}

object CreateCustomAccountEvaluator {
  implicit def createIOCreateCustomAccountEvaluator(implicit getStripeKey: GetStripePrivateKey[IO]) = new CreateCustomAccountEvaluator[IO] {
    def eval(c: CreateCustomAccountCommand): IO[Account] = {
      for {
        stripeApiKey <- getStripeKey.getKey
        _ <- IO(Stripe.apiKey = stripeApiKey.n)
        accountCreateParams = AccountCreateParams
        .builder()
        .setCountry("US")
        .setType(AccountCreateParams.Type.CUSTOM)
        .setCapabilities(
          AccountCreateParams
            .Capabilities
            .builder()
            .setCardPayments(
              AccountCreateParams.Capabilities.CardPayments.builder()
                .setRequested(true)
                .build()
            )
            .setTransfers(
              AccountCreateParams
                .Capabilities
                .Transfers
                .builder()
                .setRequested(true)
                .build()
            )
            .build()
        ).build()
        account <- IO(Account.create(accountCreateParams))       
      } yield account
    }
  }
  implicit class CreateCustomAccountEvaluatorOps(underlying: CreateCustomAccountCommand) {
    def evaluate[F[_]: CreateCustomAccountEvaluator] = implicitly[CreateCustomAccountEvaluator[F]].eval(underlying)
  }
}
