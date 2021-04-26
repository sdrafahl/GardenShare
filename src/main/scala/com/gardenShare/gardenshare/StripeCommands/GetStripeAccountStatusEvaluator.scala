package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.GetStripePrivateKey
import com.stripe.model.Account
import com.stripe.Stripe
import cats.effect.ContextShift

abstract class GetStripeAccountStatusEvaluator[F[_]] {
  def eval(x: GetStripeAccountStatus)(implicit cs: ContextShift[F]): F[Account]
}

object GetStripeAccountStatusEvaluator {
  implicit def createIOGetStripeAccountStatusEvaluator(implicit getStripeKey: GetStripePrivateKey[IO], searchStripeIdWithEmail: SearchAccountIdsByEmail[IO]) = new GetStripeAccountStatusEvaluator[IO] {
    def eval(x: GetStripeAccountStatus)(implicit cs: ContextShift[IO]): IO[Account] = {
      for {
        stripeApiKey <- getStripeKey.getKey
        _ <- IO(Stripe.apiKey = stripeApiKey.n)
        accountStatusParams <- IO(Account.retrieve(x.sellerStripeId))
      } yield accountStatusParams
    }
  }
}
