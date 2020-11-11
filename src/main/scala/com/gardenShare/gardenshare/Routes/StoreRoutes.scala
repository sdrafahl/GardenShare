package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.UserEntities.JWTValidationTokens
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser._
import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT._
import cats.implicits._
import com.gardenShare.gardenshare.UserEntities.InvalidToken
import com.gardenShare.gardenshare.UserEntities.ValidToken
import com.gardenShare.gardenshare.domain.Store.Address
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.domain.Store.CreateStoreRequest
import com.gardenShare.gardenshare.Storage.Relational.InsertStore.CreateStoreRequestOps
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import scala.util.Try
import com.gardenShare.gardenshare.GoogleMapsClient.Distance
import com.gardenShare.gardenshare.GetNearestStores.GetNearestOps

object StoreRoutes {
  def storeRoutes[F[_]: Async: com.gardenShare.gardenshare.SignupUser.SignupUser: AuthUser: AuthJWT: InsertStore: GetNearestStores: GetDistance: GetStoresStream]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "store" / "create" / address => {
        val key = CaseInsensitiveString("authentication")
        val maybeJwtHeader = req.headers
          .get(key)

        val token = maybeJwtHeader match {
          case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
          case None           => Left(Ok(NoJWTTokenProvided().asJson.toString()))
        }

        token
          .map { f =>
            f.auth
              .map {
                case InvalidToken(msg) =>
                  Ok(InvalidToken(msg).asJson.toString())
                case ValidToken(Some(email), _) => {
                  val addressOfSeller = Address(address)
                  val emailOfSeller = Email(email)
                  val request =
                    CreateStoreRequest(addressOfSeller, Email(email))
                  List(request).insertStore
                    .map(st => Ok(StoresAdded(st).asJson.toString()))
                    .flatMap(a => a)
                }
                case ValidToken(None, _) =>
                  Ok(
                    InvalidToken("Token is valid but without email").asJson
                      .toString()
                  )
              }
              .flatMap(a => a)
          }
          .fold(a => a, b => b)
      }
      case req @ GET -> Root / "store" / address / limit / rangeInSeconds => {

        val key = CaseInsensitiveString("authentication")

        val maybeJwtHeader = req.headers
          .get(key)

        val token = maybeJwtHeader match {
          case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
          case None           => Left(Ok(NoJWTTokenProvided().asJson.toString()))
        }

        val lim = Try(limit.toInt).toEither.left
          .map(a => InvalidLimitProvided(a.getMessage()))

        val range = Try(rangeInSeconds.toFloat).toEither.left
          .map(a => InvalidRangeProvided(a.getMessage()))

        token
          .map { f =>
            f.auth
              .map {
                case InvalidToken(msg) =>
                  Ok(InvalidToken(msg).asJson.toString())
                case ValidToken(Some(email), _) => {
                  lim
                    .map { li =>
                      range
                        .map { range =>
                          GetNearestStore(Distance(range), li, Address(address)).nearest
                            .map(ListOfStores)
                            .map(_.asJson.toString())
                            .attempt
                            .map(
                              _.left.map(err =>
                                ResponseBody(err.toString()).asJson.toString()
                              )
                            )
                            .map(
                              _.map(msg => ResponseBody(msg).asJson.toString())
                            )
                            .map(_.fold(a => a, b => b))
                            .map(respMsg => Ok(respMsg))
                            .flatMap(a => a)

                        }
                        .fold(fa => Ok(fa.asJson.toString()), fb => fb)
                    }
                    .fold(a => Ok(a.asJson.toString()), b => b)
                }
                case ValidToken(None, _) =>
                  Ok(
                    InvalidToken("Token is valid but without email").asJson
                      .toString()
                  )
              }
          }
          .map(_.flatMap(a => a))
          .fold(a => a, b => b)
      }
    }
  }
}
