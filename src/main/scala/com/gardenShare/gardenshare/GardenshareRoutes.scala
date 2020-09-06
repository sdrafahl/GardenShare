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
import com.gardenShare.gardenshare.UserEntities.Group
import com.gardenShare.gardenshare.Encoders.Encoders._

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
  case class UserCreationRespose(msg: String, userCreated: Boolean)
  case class AuthUserResponse(msg: String, auth: Option[AuthenticatedUser], authenticated: Boolean)
  case class IsJwtValidResponse(msg: String, valid: Boolean, groups: List[Group])

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
            case Left(err) =>  Ok(UserCreationRespose(s"User Request Failed: ${err.getMessage()}", false).asJson.toString())
            case Right(resp) => Ok(UserCreationRespose(s"User Request Made: ${resp.codeDeliveryDetails().toString()}", true).asJson.toString())
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
              case Left(error) => Ok(AuthUserResponse(s"Error Occurred: ${error}", None, false).asJson.toString())
              case Right(AuthenticatedUser(user, jwt, accToken)) =>
                Ok(AuthUserResponse("jwt token is valid", Option(AuthenticatedUser(user, jwt, accToken)), true).asJson.toString())
                .map(a => a.copy(headers = a.headers.put(Header.apply("Content-Type", "application/json"))))
                
              case Right(FailedToAuthenticate(msg)) => {
                val response = AuthUserResponse(s"User failed to verify: ${msg}",None, false)
                Ok(response.asJson.toString())
              }
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
              case Left(error) => Ok(IsJwtValidResponse(s"Error occured: ${error}", false, List()).asJson.toString())
              case Right(ValidToken(email, userGroups)) => Ok(IsJwtValidResponse("Token is valid", true, userGroups).asJson.toString())
              case Right(InvalidToken(msg)) => Ok(IsJwtValidResponse("Token is not valid", false, List()).asJson.toString())
              case Right(_) => NotAcceptable(ResponseBody("Unknown response").asJson.toString())
            }
          }
      }
    }
  }
}
