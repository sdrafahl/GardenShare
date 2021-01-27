package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.Config.GetUserPoolName
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import com.gardenShare.gardenshare.UserEntities.UserType
import com.gardenShare.gardenshare.UserEntities.Sellers
import com.gardenShare.gardenshare.Config.GetUserPoolId
import com.gardenShare.gardenshare.domain.Store.Address
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import com.gardenShare.gardenshare.domain.Store.CreateStoreRequest

abstract class ApplyUserToBecomeSeller[F[_]] {
  def applyUser(userName: Email, userType: UserType, address: Address): F[Unit]
}

object ApplyUserToBecomeSeller {
  def apply[F[_]: ApplyUserToBecomeSeller] = implicitly[ApplyUserToBecomeSeller[F]]

  implicit def createIOApplyUserToBecomeSeller(implicit gupn: GetUserPoolId[IO], cognito: CogitoClient[IO], g:GetStore[IO], insertStore:InsertStore[IO]) = new ApplyUserToBecomeSeller[IO] {
    def applyUser(userName: Email, userType: UserType, address: Address): IO[Unit] = {
      (userType match {
        case Sellers => {
          gupn
            .exec()
            .flatMap(userPoolName => cognito.addUserToGroup(userName.underlying, userPoolName, "Sellers"))
            .flatMap{_ =>
              g.getStoresByUserEmail(userName).flatMap{
                case List() => insertStore.add(List(CreateStoreRequest(address, userName)))
                case _ => IO.raiseError(new Throwable(s"Store already exists for email: ${userName}"))
              }
            }
        }
        case g => IO.raiseError(new Throwable(s"group does not exist: ${g}"))
      }).map(_ => ())
    }
  }
}
