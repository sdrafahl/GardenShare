package com.gardenShare.gardenshare.SignupUser

import cats.effect.Async
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import cats.effect.IO
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetUserPoolSecret

abstract class SignupUser[F[_]] {
  def signupUser(email: Email, password: Password)(implicit cognitoClient: CogitoClient[F], getUserPoolName:GetUserPoolName[F], getUserPoolSecret:GetUserPoolSecret[F]): F[SignUpResponse]
}

object SignupUser {
  implicit def apply[F[_]: SignupUser]() = implicitly[SignupUser[F]]

  implicit object IOSignupUser extends SignupUser[IO] {
    def signupUser(email: Email, password: Password)(implicit cognitoClient: CogitoClient[IO], getUserPoolName:GetUserPoolName[IO], getUserPoolSecret:GetUserPoolSecret[IO]): IO[SignUpResponse] = {
      for {
        id <- getUserPoolName.exec()
        result = cognitoClient.createUser(password.underlying, email.underlying, id)
      } yield result      
    }      
  }  

  implicit class SignupUserOps(underlying : User) {
    def signUp[F[_]: SignupUser:CogitoClient:GetUserPoolName:GetUserPoolSecret]() = SignupUser[F].signupUser(underlying.email, underlying.password)
  }

}
