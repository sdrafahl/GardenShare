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
import com.gardenShare.gardenshare._
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream
import com.gardenShare.gardenshare.Storage.Relational.InsertProduct
import eu.timepit.refined.types.string.NonEmptyString
import cats.Applicative
import com.gardenShare.gardenshare.GetproductDescription
import com.gardenShare.gardenshare.GetproductDescription._
import com.gardenShare.gardenshare.ParseProduce.ParseProduceOps

object ProductDescriptionRoutes {
  def productDescriptionRoutes[F[_]: Async](implicit c: GetproductDescription[Produce], p: ParseProduce[String])
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "productDescription" / descKey => {
        descKey
          .parseProduce
          .map(a => a.getProductDescription)
          .fold(a => Ok(ResponseBody("Invalid product description key was provided.", false).asJson.toString), b => Ok(b.asJson.toString))
      }
    }
  }
}
