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
import com.gardenShare.gardenshare.ProcessAndJsonResponse
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import com.gardenShare.gardenshare.domain.ProcessAndJsonResponse.ProcessData
import com.gardenShare.gardenshare.UserEntities._
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.Storage.Relational.GetWorker


object UserRoutes {
  def userRoutes[F[_]: Async: GetTypeSafeConfig: com.gardenShare.gardenshare.SignupUser.SignupUser: GetUserPoolSecret: AuthUser: GetUserPoolId: AuthJWT: GetRegion: HttpsJwksBuilder: GetDistance:GetUserPoolName: CogitoClient: GetWorker]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
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
        )
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
        )
      }
      case GET -> Root / "user" / "jwt" / jwtToken => {
        addJsonHeaders(
          ProcessData(
            JWTValidationTokens(jwtToken).auth[F],
            (jwtR:JWTValidationResult) => jwtR match {
              case ValidToken(email, userGroups) => IsJwtValidResponse("Token is valid", true, userGroups)
              case InvalidToken(msg) => IsJwtValidResponse("Token is not valid", false, List())
            },
            (error:Throwable) => IsJwtValidResponse(s"Error occured: ${error}", false, List())
          )
            .process
            .flatMap(js => Ok(js.toString()))
        )
      }
    }
  }
}
