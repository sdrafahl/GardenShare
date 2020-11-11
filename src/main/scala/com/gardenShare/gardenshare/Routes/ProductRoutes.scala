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
import com.gardenShare.gardenshare.SignupUser._
import com.gardenShare.gardenshare.SignupUser.SignupUser._
import com.gardenShare.gardenshare.GetListOfProductNames.GetListOfProductNames
import com.gardenShare.gardenshare.Storage.Relational.InsertProduct
import com.gardenShare.gardenshare.Storage.Relational.GetProductsByStore
import com.gardenShare.gardenshare.Helpers._
import com.gardenShare.gardenshare
import com.gardenShare.gardenshare.GetListOfProductNames.DescriptionName
import com.gardenShare.gardenshare.domain.Products.CreateProductRequest
import com.gardenShare.gardenshare.domain.Products.DescriptionAddress
import cats.Applicative

object ProductRoutes {
  def productRoutes[F[_]: Async: SignupUser: AuthUser: AuthJWT: InsertStore: GetNearestStores: GetDistance: GetStoresStream: GetListOfProductNames: InsertProduct: GetProductsByStore]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "product" / "create" / storeId / descripKey => {
        val insertProduct = InsertProduct[F]()

        parseJWTokenFromRequest(req)
          .map { jwtToken =>
            jwtToken.auth.map {
              case InvalidToken(msg) =>
                Left(Ok(InvalidToken(msg).asJson.toString()))
              case ValidToken(e, g) =>
                Right {
                  GetListOfProductNames().getListOfProducts.map {
                    (f: List[DescriptionName]) =>
                      f.contains(DescriptionName(descripKey)) match {
                        case false =>
                          Left(Ok(InvalidDescriptionName().asJson.toString()))
                        case true =>
                          Right {
                            storeId.toIntOption match {
                              case None =>
                                Left(
                                  Ok(InvalidStoreIDInput().asJson.toString())
                                )
                              case Some(id) =>
                                Right {
                                  insertProduct
                                    .add(
                                      List(
                                        CreateProductRequest(
                                          id,
                                          -1,
                                          DescriptionAddress(descripKey)
                                        )
                                      )
                                    )
                                    .attempt
                                    .map(
                                      _.fold(
                                        thw =>
                                          Ok(
                                            ResponseBody(thw.getMessage()).asJson
                                              .toString()
                                          ),
                                        ac => Ok(ac.asJson.toString())
                                      )
                                    )
                                }
                            }
                          }
                      }
                  }
                }
            }
          }
          .left
          .map(left => Ok(left.asJson.toString()))
          .fold(
            a => a,
            b =>
              b.flatMap(c =>
                c.fold(
                  d => d,
                  e =>
                    e.flatMap(f =>
                      f.fold(g => g, h => h.fold(le => le, ri => ri.flatten))
                    )
                )
              )
          )

      }
      case req @ GET -> Root / "product" / storeId => {

        val getProducts = GetProductsByStore[F]()

        val storeIden = storeId.toIntOption match {
          case None    => Left(Ok(InvalidStoreIDInput().asJson.toString()))
          case Some(a) => Right(a)
        }

        val key = CaseInsensitiveString("authentication")

        val maybeJwtHeader = req.headers
          .get(key)

        val token = maybeJwtHeader match {
          case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
          case None           => Left(Ok(NoJWTTokenProvided().asJson.toString()))
        }

        storeIden
          .flatMap { id =>
            token.map(_.auth).flatMap {
              case InvalidToken(msg) =>
                Left(Ok(InvalidToken(msg).asJson.toString()))
              case ValidToken(maybeEmail, groups) => {
                Right(
                  getProducts
                    .getProductsByStore(id)
                    .flatMap(lst => Ok(lst.asJson.toString()))
                )
              }
            }
          }
          .fold(a => a, b => b)
          .attempt
          .map(
            _.left.map(az =>
              Ok(ResponseBody(az.getMessage()).asJson.toString())
            )
          )
          .map(_.map(a => Applicative[F].pure(a)))
          .map(_.fold(a => a, b => b))
          .flatten
      }
    }
  }
}
