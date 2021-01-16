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
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import cats.syntax.flatMap._
import cats.FlatMap
import cats.syntax.functor._
import cats.Functor
import com.gardenShare.gardenshare.UserEntities.UserType

abstract class SignupUser[F[_]: CogitoClient: GetUserPoolName] {
  def signupUser(email: Email, password: Password): F[SignUpResponse]
}

object SignupUser {
  implicit def apply[F[_]: SignupUser]() = implicitly[SignupUser[F]]

  implicit def createSignupUser[F[_]: FlatMap: Functor](implicit cognitoClient: CogitoClient[F], getUserPoolName:GetUserPoolName[F], g: GetTypeSafeConfig[F]) = new SignupUser[F] {
    def signupUser(email: Email, password: Password): F[SignUpResponse] = {
      for {
        id <- getUserPoolName.exec()
        result = cognitoClient.createUser(password.underlying, email.underlying, id)
      } yield result      
    }      
  }
   
  implicit class SignupUserOps(underlying : User) {
    def signUp[F[_]: SignupUser:GetUserPoolName:CogitoClient]() = SignupUser[F]().signupUser(underlying.email, underlying.password)
  }
}
