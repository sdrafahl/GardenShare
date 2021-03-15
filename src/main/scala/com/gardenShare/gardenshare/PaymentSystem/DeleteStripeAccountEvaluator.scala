package com.gardenShare.gardenshare

import scala.jdk.CollectionConverters._
import cats.effect.IO
import com.stripe.model.Account
import com.gardenShare.gardenshare.GetStripePrivateKey
import com.stripe.Stripe
import java.util.HashMap
import cats.syntax.parallel._
import cats.effect.ContextShift

abstract class ClearStripeAccountsEvaluator[F[_]] {
  def eval(c: ClearStripeAccounts)(implicit cs: ContextShift[F]): F[Unit]
}

object ClearStripeAccountsEvaluator {
  implicit def createIOClearStripeAccountsEvaluator(implicit getStripeKey: GetStripePrivateKey[IO]) = new ClearStripeAccountsEvaluator[IO] {
    def eval(c: ClearStripeAccounts)(implicit cs: ContextShift[IO]): IO[Unit] = {
      for {
        stripeApiKey <- getStripeKey.getKey
        _ <- IO(Stripe.apiKey = stripeApiKey.n)
        params = new HashMap[String, Object]()
        response <- IO(Account.list(params))
        accounts = response.getData().asScala.filter(acc => c.emails.map(_.underlying).contains(acc.getEmail()) )
        _ <- accounts.map(account => IO(account.delete())).toList.parSequence
      } yield ()
    }
  }

  implicit class ClearStripeAccountsEvaluatorOps(underlying: ClearStripeAccounts) {
    def evaluate[F[_]: ClearStripeAccountsEvaluator:ContextShift] = implicitly[ClearStripeAccountsEvaluator[F]].eval(underlying)
  }
}
