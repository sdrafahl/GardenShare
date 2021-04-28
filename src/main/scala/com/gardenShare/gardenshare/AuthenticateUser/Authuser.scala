package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.GetUserPoolId
import com.gardenShare.gardenshare.GetTypeSafeConfig

abstract class AuthUser[F[_]] {
  def authTheUser(user: User)(implicit client: CogitoClient[F], getUserPoolName:GetUserPoolName[F], getUserPoolId: GetUserPoolId[F]): F[UserResponse]
}

object AuthUser {
  def apply[F[_]:AuthUser]() = implicitly[AuthUser[F]]
  implicit def createIOAuthUser(implicit get: GetTypeSafeConfig[IO]) = new AuthUser[IO] {
    def authTheUser(user: User)(implicit client: CogitoClient[IO], getUserPoolName:GetUserPoolName[IO], getUserPoolId: GetUserPoolId[IO]): IO[UserResponse] = {
      for {
        clientId <- getUserPoolName.exec()
        userPoolId <- getUserPoolId.exec()
        resp <- client.authUserAdmin(user, userPoolId.id, clientId.name).attempt
      } yield {
        resp match {
          case Right(resp) => {
            val res = resp.authenticationResult()
            AuthenticatedUser(user, res.idToken(), res.accessToken())
          }
          case Left(err) => {
            FailedToAuthenticate(s"Error: ${err.getMessage()}")
          }
        }
      }
    }
  }

  implicit class AuthUserOps[F[_]: AuthUser:CogitoClient:GetUserPoolName:GetUserPoolId](underlying: User) {
    def auth(implicit authUser: AuthUser[F]) = authUser.authTheUser(underlying)
  }
}

