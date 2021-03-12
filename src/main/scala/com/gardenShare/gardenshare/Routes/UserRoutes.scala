package com.gardenShare.gardenshare

import io.circe._, io.circe.parser._
import cats.effect.Async
import cats.effect.ContextShift
import org.http4s.Request
import com.gardenShare.gardenshare.Helpers._
import cats.ApplicativeError
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.GetTypeSafeConfig
import com.gardenShare.gardenshare.GetUserPoolSecret
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.GetUserPoolId
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.GetRegion
import com.gardenShare.gardenshare.HttpsJwksBuilder
import com.gardenShare.gardenshare.GetDistance
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.Password
import com.gardenShare.gardenshare.User
import com.gardenShare.gardenshare.JWTValidationTokens
import org.http4s.HttpRoutes
import com.gardenShare.gardenshare.SignupUser._
import cats.implicits._
import io.circe.generic.auto._, io.circe.syntax._
import org.http4s.Header
import com.gardenShare.gardenshare.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.AuthenticatedUser
import com.gardenShare.gardenshare.FailedToAuthenticate
import com.gardenShare.gardenshare.ValidToken
import com.gardenShare.gardenshare.Encoders._
import com.gardenShare.gardenshare.InvalidToken
import com.gardenShare.gardenshare.ProcessAndJsonResponse
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.UserResponse
import com.gardenShare.gardenshare.JWTValidationResult
import cats.Applicative
import com.gardenShare.gardenshare.Sellers
import com.gardenShare.gardenshare.domain._
import com.gardenShare.gardenshare.GetUserInfo.GetUserInfoOps
import com.gardenShare.gardenshare.UserInfo
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.AddressNotProvided
import com.gardenShare.gardenshare.Helpers.ResponseHelper
import _root_.fs2.text

object UserRoutes {
  def userRoutes[F[_]: Async: GetTypeSafeConfig:GetUserInfo: com.gardenShare.gardenshare.SignupUser: GetUserPoolSecret: AuthUser: GetUserPoolId: AuthJWT: GetRegion: HttpsJwksBuilder: GetDistance:GetUserPoolName: CogitoClient:ApplyUserToBecomeSeller: ContextShift]()
      : HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case POST -> Root / "user" / "signup" / email / password => {
        val emailToPass = Email(email)
        val passwordToPass = Password(password)
        val user = User(emailToPass, passwordToPass)

        addJsonHeaders(
          ProcessData(
            user.signUp[F](),
            (resp:SignUpResponse) => UserCreationRespose(
              s"User Request Made: ${resp.codeDeliveryDetails().toString()}",
              true
            ),
            (err:Throwable) => UserCreationRespose(
              s"User Request Failed: ${err.getMessage()}",
              false
            )
          )
            .process
            .flatMap(js => Ok(js.toString()))
        ).catchError
      }

      case GET -> Root / "user" / "auth" / email / password => {
        addJsonHeaders(ProcessData(
          User(Email(email), Password(password)).auth,
          (usr:UserResponse) => usr match {
            case AuthenticatedUser(user, jwt, accToken) => AuthUserResponse(
              "jwt token is valid",
              Option(AuthenticatedUser(user, jwt, accToken)),
              true
            )
            case FailedToAuthenticate(msg) => AuthUserResponse(s"User failed to verify: ${msg}", None, false)
          },
          (error: Throwable) => AuthUserResponse(s"Error Occurred: ${error}", None, false)
        )
          .process
          .flatMap(js => Ok(js.toString()))
        ).catchError
      }
      case GET -> Root / "user" / "jwt" / jwtToken => {
        addJsonHeaders(
          ProcessData(
            JWTValidationTokens(jwtToken).auth[F],
            (jwtR:JWTValidationResult) => jwtR match {
              case ValidToken(email) => IsJwtValidResponse("Token is valid", true)
              case InvalidToken(msg) => IsJwtValidResponse("Token is not valid", false)
            },
            (error:Throwable) => IsJwtValidResponse(s"Error occured: ${error}", false)
          )
            .process
            .flatMap(js => Ok(js.toString()))
        ).catchError
      }
      case req @ POST -> Root / "user" / "apply-to-become-seller" => {
        parseREquestAndValidateUserAndParseBodyResponse[Address,F](req, {(email, address) =>
          ProcessData(
            implicitly[ApplyUserToBecomeSeller[F]].applyUser(email, Sellers, address),
            (_: Unit) => ResponseBody("User is now a seller", true),
            (err:Throwable) => ResponseBody(err.getMessage(), false)
          )
            .process
        })
      }
      case req @ GET -> Root / "user" / "info" => {
        parseRequestAndValidateUserResponse[F](req, {email =>
          ProcessData(
            email.getUserInfo,
            (a:UserInfo) => a.asJson,
            (err:Throwable) => FailedToGetUserInfo(err.getMessage())
          )
            .process
        })
      }
    }
  }
}
