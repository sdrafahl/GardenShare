package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.CogitoClient._
import com.gardenShare.gardenshare.CogitoClient
import cats.effect.IO
import com.gardenShare.gardenshare.User
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.Password
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.GetUserPoolSecret
import com.gardenShare.gardenshare.CogitoClient._
import com.gardenShare.gardenshare.GetTypeSafeConfig
import cats.syntax.flatMap._
import cats.FlatMap
import cats.syntax.functor._
import cats.Functor

abstract class SignupUser[F[_]] {
  def signupUser(email: Email, password: Password): F[SignUpResponse]
}

object SignupUser {
  def apply[F[_]: SignupUser]() = implicitly[SignupUser[F]]

  implicit def createIOSignupUser(implicit cognitoClient: CogitoClient[IO], getUserPoolName:GetUserPoolName[IO], g: GetTypeSafeConfig[IO]) = new SignupUser[IO] {
    def signupUser(email: Email, password: Password): IO[SignUpResponse] = {
      for {
        id <- getUserPoolName.exec()
        result = cognitoClient.createUser(password.underlying, email.underlying.value, id)
      } yield result      
    }      
  }
   
  implicit class SignupUserOps(underlying : User) {
    def signUp[F[_]: SignupUser:GetUserPoolName:CogitoClient]() = SignupUser[F]().signupUser(underlying.email, underlying.password)
  }
}
