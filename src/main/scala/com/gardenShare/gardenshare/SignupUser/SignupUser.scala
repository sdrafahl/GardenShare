package com.gardenShare.gardenshare.SignupUser

import cats.effect.Async
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import cats.effect.IO
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password

abstract class SignupUser[F[_]: Async] {
  def signupUser(email: Email, password :Password)(implicit cognitoClient: CogitoClient[F]): F[Unit]
}

object SignupUser {
  implicit def apply[F[_]: SignupUser]() = implicitly[SignupUser[F]]
  implicit object IOSignupUser extends SignupUser[IO] {
    def signupUser(email: Email, password: Password)(implicit cognitoClient: CogitoClient[IO]): IO[Unit] = cognitoClient.createUser(password.underlying, email.underlying).map(_ =>())
  }

  implicit class SignupUserOps[F[_]: SignupUser: CogitoClient](underlying : User) {
    def signUp() = SignupUser[F].signupUser(underlying.email, underlying.password)
  }

}
