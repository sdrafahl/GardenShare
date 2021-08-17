package com.gardenShare.gardenshare

import cats.effect.Async
import cats.effect.ContextShift
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.GetUserPoolId
import com.gardenShare.gardenshare.AuthJWT
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.Password
import com.gardenShare.gardenshare.User
import com.gardenShare.gardenshare.JWTValidationTokens
import org.http4s.HttpRoutes
import com.gardenShare.gardenshare.SignupUser._
import cats.implicits._
import io.circe.generic.auto._
import com.gardenShare.gardenshare.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.JWTValidationResult
import com.gardenShare.gardenshare.GetUserInfo.GetUserInfoOps
import com.gardenShare.gardenshare.Address
import scala.concurrent.ExecutionContext
import JWTValidationResult._
import com.gardenShare.gardenshare.UserResponse._
import AuthenticateJWTOnRequest.AuthenticateJWTOnRequestOps
import org.http4s.circe.CirceEntityCodec._
import ProcessPolymorphicType.ProcessPolymorphicTypeOps
import com.gardenShare.gardenshare.SignupUser

object UserRoutes {
  def userRoutes[F[_]:
      Async:
      GetUserInfo:
      SignupUser:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetUserPoolName:
      CogitoClient:
      ApplyUserToBecomeSeller:
      ContextShift:
      VerifyUserAsSeller:
      ProcessPolymorphicType
  ]()(
    implicit ec: ExecutionContext
  )
      : HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case POST -> Root / "user" / "signup" / Email(emailToPass) / Password(passwordToPass) => {
        val user = User(emailToPass, passwordToPass)
        (for {
          signUpResponse <- user.signUp[F]()          
        } yield UserCreationRespose(s"User Request Made: ${signUpResponse.codeDeliveryDetails()}", true))
          .asJsonF          
      }
      case GET -> Root / "user" / "auth" / Email(email) / Password(password) => {
        val user = User(email, password)
        (for {
          authenticatedUser <- user.auth
        } yield authenticatedUser match {
          case AuthenticatedUser(user, jwt, accToken) => AuthUserResponse("jwt token is valid",Option(AuthenticatedUser(user, jwt, accToken)),true)
          case FailedToAuthenticate(msg) => AuthUserResponse(s"User failed to verify: ${msg}", None, false)
        }).asJsonF
      }       
      case GET -> Root / "user" / "jwt" / JWTValidationTokens(jwtToken) => {
        (for {
          authResponse <- jwtToken.auth[F]
        } yield authResponse match {
          case ValidToken(_) => IsJwtValidResponse("Token is valid", true)
          case InvalidToken(_) => IsJwtValidResponse("Token is not valid", false)
        }).asJsonF        
      }
      case req @ POST -> Root / "user" / "apply-to-become-seller" => {
        for {
          emailOfUser <- req.authJWT
          applyUserToBecomeSeller <- req.as[ApplyUserToBecomeSellerData]
          applyUserToBecomeSellerResponse <- implicitly[ApplyUserToBecomeSeller[F]]
          .applyUser(emailOfUser, applyUserToBecomeSeller.address, applyUserToBecomeSeller.refreshUrl ,applyUserToBecomeSeller.returnUrl).asJsonF
        } yield applyUserToBecomeSellerResponse
      }
      case req @ POST -> Root / "user" / "verify-user-as-seller" => {        
        (for {
          email <- req.authJWT
          address <- req.as[Address]
          isValidated <- VerifyUserAsSeller[F]().verify(email, address)          
        } yield isValidated match {
          case true => ResponseBody("User is now a seller and address was set", true)
          case false => ResponseBody("User has not created completed flow and is not a user", false)
        }).asJsonF        
      }
      case req @ GET -> Root / "user" / "info" => {
        for {
          email <- req.authJWT
          response <- email.getUserInfo.asJsonF
        } yield response        
      }            
    }
  }
}
