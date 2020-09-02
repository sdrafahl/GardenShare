package com.gardenShare.gardenshare

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.Encryption.Decrypt
import com.gardenShare.gardenshare.Config.GetPrivateKey
import org.apache.commons.codec.binary.Base64
import com.gardenShare.gardenshare.SignupUser.SignupUser._
import com.gardenShare.gardenshare.SignupUser.SignupUser
import cats.effect.IO
import cats.effect.Async
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Storage.Users.Cognito._
import com.gardenShare.gardenshare.SignupUser.SignupUser._
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetUserPoolSecret
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.UserEntities.AuthenticatedUser
import com.gardenShare.gardenshare.UserEntities.FailedToAuthenticate
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.Config.GetUserPoolId
import com.gardenShare.gardenshare.UserEntities.UserResponse
import io.circe.generic.auto._, io.circe.syntax._

object GardenshareRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  def userRoutes[F[_]: Async:CogitoClient:GetUserPoolName:GetTypeSafeConfig:SignupUser:GetUserPoolSecret:AuthUser:GetUserPoolId](): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case POST -> Root / "user" / "signup" / email / password => {
        val emailToPass = Email(email)
        val passwordToPass = Password(password)
        val user = User(emailToPass,passwordToPass)
        for {
          resp <- user.signUp[F]()
          success = resp.userConfirmed().booleanValue()
          newResp <- success match {
            case true => Ok("User Request Made")
            case false => NotAcceptable(s"User Request Failed: ${resp.toString()}")
          }
        } yield newResp
      }

      case GET -> Root / "user" / "auth" / email / password => {
        User(Email(email), Password(password))
          .auth
          .flatMap{mr =>
            mr match {
              case AuthenticatedUser(user, jwt, accToken) => Ok(AuthenticatedUser(user, jwt, accToken).asJson.toString())
              case FailedToAuthenticate(msg) => NotAcceptable(s"User failed to verify: ${msg}")
            }
          }
      }
    }
  }
}
