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
import com.gardenShare.gardenshare.Storage.S3.GetS3Stream
import com.gardenShare.gardenshare.ParseDescription.ParseDescriptionStream
import com.gardenShare.gardenshare.GetProductDescription.GetproductDescription
import eu.timepit.refined.types.string.NonEmptyString
import com.gardenShare.gardenshare.domain.Entities.GetproductDescriptionCommand
import com.gardenShare.gardenshare.domain.Entities.GetproductDescriptionCommand._
import cats.Applicative
import com.gardenShare.gardenshare.GetProductDescription.GetproductDescription._
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.Orders.CreateOrder
import com.gardenShare.gardenshare.Storage.Relational.AddOrderIdToProduct
import com.gardenShare.gardenshare.Storage.Relational.GetProductByID
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import com.gardenShare.gardenshare.Storage.Relational.GetStoreByID
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.domain.Orders.CreateOrderCommand
import com.gardenShare.gardenshare.Orders.CreateOrder._

object OrderRoutes {
  def orderRoutes[F[_]: Async: CreateOrder: AuthJWT: AddOrderIdToProduct: GetProductByID: GetStore: GetStoreByID: ContextShift]()(implicit apperr: ApplicativeError[F, Throwable]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "orders" => {
        val maybeProductId = (req.headers.get(CaseInsensitiveString("productIds")) match {
          case None => Left(InvalidProductId("no product ids given").asJson.toString())
          case Some(h) => Right(parse(h.value).map(_.as[List[Int]]).left.map(pe =>  InvalidProductId(pe.message).asJson.toString()))
        }).fold(l =>  apperr.raiseError(new Throwable(l)), r => r.fold(l1 => apperr.raiseError(new Throwable(l1)), r1 => r1.fold(f => apperr.raiseError(new Throwable(f.getMessage())), r2 =>  Applicative.apply[F].pure(r2))))


        val pgmToAddOrder = parseJWTokenFromRequest(req)
          .left
          .map(e => Ok(e.asJson.toString()))
          .map{a => a.auth}
          .map(_.map{
            case InvalidToken(msg) => Left(Ok(InvalidToken(msg).asJson.toString()))
            case ValidToken(None, _) => Left(Ok(NoEmail().asJson.toString()))
            case ValidToken(Some(email), groups) => {
              Right(Ok(for {
                prodIds <- maybeProductId
                af <- CreateOrderCommand(prodIds).create
              } yield af.asJson.toString()))
            }
            case _ => Left(Ok(ResponseBody("Invalid").asJson.toString()))
          })

        pgmToAddOrder.fold(l1 => l1, r1 => r1.flatMap(_.fold(l2 => l2, r2 => r2)))
      }
    }
  }
}
