package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.Config.GetUserPoolName
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import com.gardenShare.gardenshare.UserEntities.UserType
import com.gardenShare.gardenshare.UserEntities.Sellers

abstract class ApplyUserToBecomeSeller[F[_]] {
  def applyUser(userName: Email, userType: UserType): F[Unit]
}

object ApplyUserToBecomeSeller {
  def apply[F[_]: ApplyUserToBecomeSeller] = implicitly[ApplyUserToBecomeSeller[F]]

  implicit def createIOApplyUserToBecomeSeller(implicit gupn: GetUserPoolName[IO], cognito: CogitoClient[IO]) = new ApplyUserToBecomeSeller[IO] {
    def applyUser(userName: Email, userType: UserType): IO[Unit] = {
      userType match {
        case Sellers => gupn.exec().flatMap(userPoolName => cognito.addUserToGroup(userName.underlying, userPoolName, Sellers.toString)).map(_ => ())
        case g => IO.raiseError(new Throwable(s"group does not exist: ${g}"))
      }
    }
  }
}
