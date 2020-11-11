package com.gardenShare.gardenshare

import io.circe._, io.circe.parser._
import cats.effect.Async
import cats.effect.ContextShift
import org.http4s.Request
import com.gardenShare.gardenshare.Helpers._
import cats.ApplicativeError
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import com.gardenShare.gardenshare.Config.GetUserPoolSecret
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.Config.GetUserPoolId
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.Config.GetRegion
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.HttpsJwksBuilder
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.JWTValidationTokens
import org.http4s.HttpRoutes
import com.gardenShare.gardenshare.SignupUser.SignupUser._
import cats.implicits._
import io.circe.generic.auto._, io.circe.syntax._
import org.http4s.Header
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.UserEntities.AuthenticatedUser
import com.gardenShare.gardenshare.UserEntities.FailedToAuthenticate
import com.gardenShare.gardenshare.UserEntities.ValidToken
import com.gardenShare.gardenshare.Encoders.Encoders._
import com.gardenShare.gardenshare.UserEntities.InvalidToken

object UserRoutes {
  def userRoutes[F[_]: Async: GetTypeSafeConfig: com.gardenShare.gardenshare.SignupUser.SignupUser: GetUserPoolSecret: AuthUser: GetUserPoolId: AuthJWT: GetRegion: HttpsJwksBuilder: GetDistance:GetUserPoolName: CogitoClient]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case POST -> Root / "user" / "signup" / email / password => {
        val emailToPass = Email(email)
        val passwordToPass = Password(password)
        val user = User(emailToPass, passwordToPass)
        val processRequest = (for {
          resp <- user.signUp[F]()
        } yield resp).attempt
        for {
          result <- processRequest
          newResp <- result match {
            case Left(err) =>
              Ok(
                UserCreationRespose(
                  s"User Request Failed: ${err.getMessage()}",
                  false
                ).asJson.toString()
              )
            case Right(resp) =>
              Ok(
                UserCreationRespose(
                  s"User Request Made: ${resp.codeDeliveryDetails().toString()}",
                  true
                ).asJson.toString()
              ).map(a =>
                a.copy(headers =
                  a.headers.put(
                    Header.apply("Content-Type", "application/json")
                  )
                )
              )
          }
        } yield newResp
      }

      case GET -> Root / "user" / "auth" / email / password => {
        val result = User(Email(email), Password(password)).auth.attempt

        result.flatMap { mr =>
          mr match {
            case Left(error) =>
              Ok(
                AuthUserResponse(s"Error Occurred: ${error}", None, false).asJson
                  .toString()
              )
            case Right(AuthenticatedUser(user, jwt, accToken)) =>
              Ok(
                AuthUserResponse(
                  "jwt token is valid",
                  Option(AuthenticatedUser(user, jwt, accToken)),
                  true
                ).asJson.toString()
              ).map(a =>
                a.copy(headers =
                  a.headers.put(
                    Header.apply("Content-Type", "application/json")
                  )
                )
              )

            case Right(FailedToAuthenticate(msg)) => {
              val response =
                AuthUserResponse(s"User failed to verify: ${msg}", None, false)
              Ok(response.asJson.toString())
            }
            case _ =>
              NotAcceptable(ResponseBody(s"Unknown response").asJson.toString())
          }
        }
      }
      case GET -> Root / "user" / "jwt" / jwtToken => {
        val result = JWTValidationTokens(jwtToken)
          .auth[F]
          .attempt

        result.flatMap { rest =>
          rest match {
            case Left(error) =>
              Ok(
                IsJwtValidResponse(s"Error occured: ${error}", false, List()).asJson
                  .toString()
              )
            case Right(ValidToken(email, userGroups)) =>
              Ok(
                IsJwtValidResponse("Token is valid", true, userGroups).asJson
                  .toString()
              )
            case Right(InvalidToken(msg)) =>
              Ok(
                IsJwtValidResponse("Token is not valid", false, List()).asJson
                  .toString()
              )
            case Right(_) =>
              NotAcceptable(ResponseBody("Unknown response").asJson.toString())
          }
        }
      }
    }
  }
}
