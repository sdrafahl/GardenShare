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
import com.gardenShare.gardenshare._
import com.gardenShare.gardenshare.InsertStore
import com.gardenShare.gardenshare.GetStoresStream
import com.gardenShare.gardenshare.InsertProduct
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
