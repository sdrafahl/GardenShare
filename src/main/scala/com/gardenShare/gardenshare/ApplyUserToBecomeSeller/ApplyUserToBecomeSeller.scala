package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Email
import cats.effect.IO
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.UserType
import com.gardenShare.gardenshare.Sellers
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.InsertStore
import com.gardenShare.gardenshare.CreateStoreRequest
import com.gardenShare.gardenshare.PaymentCommandEvaluator.PaymentCommandEvaluatorOps

abstract class ApplyUserToBecomeSeller[F[_]] {
  def applyUser(userName: Email, userType: UserType, address: Address): F[Unit]
}

object ApplyUserToBecomeSeller {
  def apply[F[_]: ApplyUserToBecomeSeller] = implicitly[ApplyUserToBecomeSeller[F]]

  implicit def createIOApplyUserToBecomeSeller(
    implicit gupn: GetUserPoolId[IO],
    cognito: CogitoClient[IO],
    g:GetStore[IO],
    insertStore:InsertStore[IO],
    paymentCommandEvaluator: PaymentCommandEvaluator[IO]) = new ApplyUserToBecomeSeller[IO] {
    def applyUser(userName: Email, userType: UserType, address: Address): IO[Unit] = {
      (userType match {
        case Sellers => {
          for {
            _ <- CreateStripeConnectedAccount(userName).evaluate
          } yield ???
          


          // stuff to add user to seller group
          // gupn
          //   .exec()
          //   .flatMap(userPoolName => cognito.addUserToGroup(userName.underlying, userPoolName, "Sellers"))
          //   .flatMap{_ =>
          //     g.getStoresByUserEmail(userName).flatMap{
          //       case List() => insertStore.add(List(CreateStoreRequest(address, userName)))
          //       case _ => IO.raiseError(new Throwable(s"Store already exists for email: ${userName}"))
          //     }
          //   }
          ???
        }
        case g => IO.raiseError(new Throwable(s"group does not exist: ${g}"))
      }).map(_ => ())
    }
  }
}
