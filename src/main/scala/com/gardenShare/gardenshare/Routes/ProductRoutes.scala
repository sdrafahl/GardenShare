package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.GetDistance
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.JWTValidationTokens
import com.gardenShare.gardenshare.AuthUser
import io.circe._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.AuthJWT._
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.InsertProduct
import com.gardenShare.gardenshare.Helpers._
import cats.Applicative
import cats.effect.ContextShift
import cats.ApplicativeError
import cats.Monad
import cats.implicits._
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetProductsByStore
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import com.gardenShare.gardenshare.Helpers.ResponseHelper

object ProductRoutes {
  def productRoutes[F[_]:
      Async:
      AuthUser:
      AuthJWT:
      GetDistance:
      InsertProduct:
      AddProductToStoreForSeller:
      GetUserInfo:
      AddProductToStore:
      ContextShift:
      Monad:
      GetProductsSoldFromSeller:
      GetStore:
      GetProductsByStore:
      JoseProcessJwt
  ]
    (      
      implicit  ae: ApplicativeError[F, Throwable],
      en: Encoder[Produce],
      currencyEncoder: Encoder[Currency],
      dep: Decoder[Produce],
      dee: Decoder[Currency]
    )
      : HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "product" / "add" / Produce(produce) / Price(price) / Currency(currency) => {
        parseRequestAndValidateUserResponse[F](req, {email =>
          ProcessData(
            implicitly[AddProductToStoreForSeller[F]].add(email, produce, Amount(price, currency)),
            (_: Unit) => ResponseBody("Product was added to the store", true),
            (err: Throwable) => ResponseBody(s"There was an error adding produce ${err.getMessage()}", false)
          ).process
        })
      }
      case req @ GET -> Root / "product" => {
        parseRequestAndValidateUserResponse[F](req, {email =>
          ProcessData(
            implicitly[GetProductsSoldFromSeller[F]].get(email),
            (a:List[ProductWithId]) => ListOfProduce(a).asJson,
            (err:Throwable) => ResponseBody(s"Failed to get products from seller: ${err.getMessage()}", false))
            .process
        })        
      }
      case GET -> Root / "product" / email => {
        implicitly[com.gardenShare.gardenshare.Parser[Email]].parse(email) match {
          case Left(_) => Ok(ResponseBody("Invalid email provided", false).asJson.toString())
          case Right(email) => {
            ProcessData(
              implicitly[GetProductsSoldFromSeller[F]].get(email),
              (a:List[ProductWithId]) => ListOfProduce(a).asJson,
              (err:Throwable) => ResponseBody(s"Failed to get products from seller: ${err.getMessage()}", false)
            )
              .process
              .flatMap(a => Ok(a.toString()))
              .catchError
          }
        }        
      }      
    }
  }
}
