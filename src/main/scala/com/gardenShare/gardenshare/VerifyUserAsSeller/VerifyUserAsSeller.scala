package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.PaymentCommandEvaluator.PaymentCommandEvaluatorOps

abstract class VerifyUserAsSeller[F[_]] {
  def verify(email: Email, address: Address): F[Boolean]
}

object VerifyUserAsSeller {
  def apply[F[_]: VerifyUserAsSeller]() = implicitly[VerifyUserAsSeller[F]]

  implicit def createIOVerifyUserAsSeller(
    implicit paymentCommandEvaluator: PaymentCommandEvaluator[IO],
    gupn: GetUserPoolId[IO],
    cognito: CogitoClient[IO],
    insertStore:InsertStore[IO],
    g:GetStore[IO],
    searchByEmail: SearchAccountIdsByEmail[IO]
  ) = new VerifyUserAsSeller[IO] {
    def verify(email: Email, address: Address): IO[Boolean] = {
      for {
        maybeSlickID <- searchByEmail.search(email)
        slickId <- maybeSlickID match {
          case None => IO.raiseError(new Throwable("Slick ID reference doesn't exist"))
          case Some(id) => IO.pure(id)
        }
        account <- GetStripeAccountStatus(slickId).evaluate
        isASeller <- account.getDetailsSubmitted().booleanValue match {
          case true => {
            for {
              userPoolId <- gupn.exec()
              _ <- cognito.addUserToGroup(email.underlying.value,userPoolId, "Sellers")
              storesThatBelongToUser <- g.getStoresByUserEmail(email)
              responseFromAddingStore <- storesThatBelongToUser match {
                case List() => insertStore.add(List(CreateStoreRequest(address, email))).map(_ => true)
                case _ => IO.raiseError(new Throwable(s"Store already exists for email: ${email}"))
              }
            } yield responseFromAddingStore
          }
          case false => IO.pure(false)
        }
      } yield isASeller
    }
  }
}

