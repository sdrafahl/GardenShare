package com.gardenShare.gardenshare.authenticateUser.AuthUser

import cats.effect.IO
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import cats.effect.Async
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.UserEntities._
import scala.jdk.OptionConverters._
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetUserPoolId
import scala.util.Try

abstract class AuthUser[F[_]] {
  def authTheUser(user: User)(implicit client: CogitoClient[F], getUserPoolName:GetUserPoolName[F], getUserPoolId: GetUserPoolId[F]): F[UserResponse]
}

object AuthUser {
  implicit def apply[F[_]:AuthUser]() = implicitly[AuthUser[F]]
  implicit object IOAuthUser extends AuthUser[IO] {
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

