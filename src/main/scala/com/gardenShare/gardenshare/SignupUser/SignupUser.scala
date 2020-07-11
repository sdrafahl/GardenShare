package com.gardenShare.gardenshare.SignupUser

import cats.effect.Async
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import cats.effect.IO
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse

abstract class SignupUser[F[_]] {
  def signupUser(email: Email, password :Password)(implicit cognitoClient: CogitoClient[F]): F[SignUpResponse]
}

object SignupUser {
  implicit def apply[F[_]: SignupUser]() = implicitly[SignupUser[F]]

  implicit object IOSignupUser extends SignupUser[IO] {
    def signupUser(email: Email, password: Password)(implicit cognitoClient: CogitoClient[IO]): IO[SignUpResponse] = cognitoClient.createUser(password.underlying, email.underlying)
  }  

  implicit class SignupUserOps(underlying : User) {
    def signUp[F[_]: SignupUser: CogitoClient]() = SignupUser[F].signupUser(underlying.email, underlying.password)
  }

}
