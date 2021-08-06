package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.AuthJWT
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.Helpers._
import cats.effect.ContextShift
import cats.implicits._
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetProductsByStore
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import com.gardenShare.gardenshare.Helpers.ResponseHelper

object ProductRoutes {
  def productRoutes[F[_]:
      Async:
      AuthJWT:
      AddProductToStoreForSeller:
      GetUserInfo:
      AddProductToStore:
      ContextShift:
      GetProductsSoldFromSeller:
      GetStore:
      GetProductsByStore
  ]: HttpRoutes[F] = {
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
      case GET -> Root / "product" / Email(email) => {
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
