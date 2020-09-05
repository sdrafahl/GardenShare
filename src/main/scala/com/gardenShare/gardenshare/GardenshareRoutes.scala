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
import java.time.LocalDate
import com.gardenShare.gardenshare.UserEntities.JWTValidationTokens
import com.gardenShare.gardenshare.UserEntities.InvalidToken
import com.gardenShare.gardenshare.UserEntities.ValidToken
import com.gardenShare.gardenshare.Config.GetRegion
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.HttpsJwksBuilder
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT.AuthJwtOps
import org.http4s.dsl.impl.Responses.BadRequestOps
import io.circe.generic.auto._, io.circe.syntax._
import org.http4s.Header

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

  case class ResponseBody(msg: String)

  def userRoutes[F[_]:
      Async:
      CogitoClient:
      GetUserPoolName:
      GetTypeSafeConfig:
      SignupUser:
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder
  ](): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case POST -> Root / "user" / "signup" / email / password => {
        val emailToPass = Email(email)
        val passwordToPass = Password(password)
        val user = User(emailToPass,passwordToPass)
        val processRequest = (for {
          resp <- user.signUp[F]()
        } yield resp)
          .attempt
        for {
          result <- processRequest
          newResp <- result match {
            case Left(err) =>  Conflict(ResponseBody(s"User Request Failed: ${err.getMessage()}").asJson.toString())
            case Right(resp) => Ok(ResponseBody(s"User Request Made: ${resp.codeDeliveryDetails().toString()}").asJson.toString())
              .map(a => a.copy(headers = a.headers.put(Header.apply("Content-Type", "application/json"))))
          }
        } yield newResp
      }

      case GET -> Root / "user" / "auth" / email / password => {
        val result =  User(Email(email), Password(password))
          .auth
          .attempt

          result.flatMap{mr =>
            mr match {
              case Left(error) => NotAcceptable(ResponseBody(s"Error Occurred: ${error}").asJson.toString())
              case Right(AuthenticatedUser(user, jwt, accToken)) =>
                Ok(ResponseBody(AuthenticatedUser(user, jwt, accToken).asJson.toString()).asJson.toString())
                .map(a => a.copy(headers = a.headers.put(Header.apply("Content-Type", "application/json"))))
                
              case Right(FailedToAuthenticate(msg)) => NotAcceptable(ResponseBody(s"User failed to verify: ${msg}").asJson.toString())
              case _ => NotAcceptable(ResponseBody(s"Unknown response").asJson.toString())
            }
          }
      }
      case GET -> Root / "user" / "jwt" / jwtToken => {
        val result = JWTValidationTokens(jwtToken)
          .auth[F]
          .attempt

          result.flatMap {rest =>
            rest match {
              case Left(error) => NotAcceptable(ResponseBody(s"Error occured: ${error}").asJson.toString())
              case Right(ValidToken()) => Ok(ResponseBody("Token is valid").asJson.toString())
              case Right(InvalidToken(msg)) => Ok(ResponseBody("Token is not valid").asJson.toString())
              case Right(_) => NotAcceptable(ResponseBody("Unknown response").asJson.toString())
            }
          }
      }
    }
  }
}
