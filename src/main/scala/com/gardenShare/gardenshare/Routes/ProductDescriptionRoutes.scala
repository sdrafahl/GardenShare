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
import com.gardenShare.gardenshare.ParseDescription.ParseDescriptionStream
import eu.timepit.refined.types.string.NonEmptyString
import com.gardenShare.gardenshare.domain.Entities.GetproductDescriptionCommand
import com.gardenShare.gardenshare.domain.Entities.GetproductDescriptionCommand._
import cats.Applicative
import com.gardenShare.gardenshare.GetProductDescription.GetproductDescription
import com.gardenShare.gardenshare.GetProductDescription.GetproductDescription._

import com.gardenShare.gardenshare.Concurrency.Concurrency._

object ProductDescriptionRoutes {
  def productDescriptionRoutes[F[_]: Async: com.gardenShare.gardenshare.SignupUser.SignupUser: AuthUser: AuthJWT: InsertStore: GetNearestStores: GetDistance: GetStoresStream: com.gardenShare.gardenshare.GetListOfProductNames.GetListOfProductNames: InsertProduct: ParseDescriptionStream: GetproductDescription: ContextShift]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "productDescription" / descKey => {
        NonEmptyString
          .from(descKey)
          .map(GetproductDescriptionCommand)
          .map(_.getDesc[F])
          .fold(
            errMsg =>
              Applicative[F].pure(ResponseBody(errMsg).asJson.toString()),
            ab => ab.map(_.asJson.toString())
          )
          .attempt
          .map(
            _.fold(
              errMsg => ResponseBody(errMsg.getMessage()).asJson.toString(),
              a => a
            )
          )
          .flatMap(Ok(_))
      }
    }
  }
}
