package com.gardenShare.gardenshare

import cats.effect.Async
import cats.effect.ContextShift
import com.gardenShare.gardenshare.Helpers._
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.GetTypeSafeConfig
import com.gardenShare.gardenshare.GetUserPoolSecret
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
import com.gardenShare.gardenshare.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.UserResponse
import com.gardenShare.gardenshare.JWTValidationResult
import com.gardenShare.gardenshare.GetUserInfo.GetUserInfoOps
import com.gardenShare.gardenshare.UserInfo
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.Helpers.ResponseHelper
import scala.concurrent.ExecutionContext
import JWTValidationResult._
import com.gardenShare.gardenshare.UserResponse._
import AuthenticateJWTOnRequest.AuthenticateJWTOnRequestOps
import org.http4s.circe.CirceEntityCodec._
import ProcessPolymorphicType.ProcessPolymorphicTypeOps

object UserRoutes {
  def userRoutes[F[_]:
      Async:
      GetTypeSafeConfig:
      GetUserInfo:
      com.gardenShare.gardenshare.SignupUser:
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder:
      GetDistance:
      GetUserPoolName:
      CogitoClient:
      ApplyUserToBecomeSeller:
      ContextShift:
      VerifyUserAsSeller:
      JoseProcessJwt
  ]()(
    implicit ec: ExecutionContext
  )
      : HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case POST -> Root / "user" / "signup" / Email(emailToPass) / Password(passwordToPass) => {
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

      case GET -> Root / "user" / "auth" / Email(email) / Password(password) => {       
        addJsonHeaders(ProcessData(
          User(email, password).auth,
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
      case GET -> Root / "user" / "jwt" / JWTValidationTokens(jwtToken) => {
        addJsonHeaders(
          ProcessData(
            jwtToken.auth[F],
            (jwtR:JWTValidationResult) => jwtR match {
              case ValidToken(_) => IsJwtValidResponse("Token is valid", true)
              case InvalidToken(_) => IsJwtValidResponse("Token is not valid", false)
            },
            (error:Throwable) => IsJwtValidResponse(s"Error occured: ${error}", false)
          )
            .process
            .flatMap(js => Ok(js.toString()))
        ).catchError
      }
      case req @ POST -> Root / "user" / "apply-to-become-seller" => {
        for {
          emailOfUser <- req.authJWT
          applyUserToBecomeSeller <- req.as[ApplyUserToBecomeSellerData]
          applyUserToBecomeSellerResponse <- implicitly[ApplyUserToBecomeSeller[F]].applyUser(emailOfUser, applyUserToBecomeSeller.address, applyUserToBecomeSeller.refreshUrl ,applyUserToBecomeSeller.returnUrl).asJsonF
        } yield applyUserToBecomeSellerResponse
      }
      case req @ POST -> Root / "user" / "verify-user-as-seller" => {
        parseREquestAndValidateUserAndParseBodyResponse[Address, F](req, {(email, address) =>
          ProcessData(
            VerifyUserAsSeller[F]().verify(email, address),
            (resp: Boolean) => resp match {
              case true => ResponseBody("User is now a seller and address was set", true)
              case false => ResponseBody("User has not created completed flow and is not a user", false)
            },
            (err:Throwable) => ResponseBody(s"There was an error validating user, Error: ${err.getMessage()}", false)
          ).process
        })
      }
      case req @ GET -> Root / "user" / "info" => {
        parseRequestAndValidateUserResponse[F](req, {email =>
          ProcessData(
            email.getUserInfo,
            (a:UserInfo) => a.asJson,
            (err:Throwable) => ResponseBody(s"Failed to get user info ${err.getMessage()}", false)
          )
            .process
        })
      }            
    }
  }
}
