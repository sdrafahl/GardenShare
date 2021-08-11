package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.AuthJWT
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import com.gardenShare.gardenshare.Email
import cats.effect.ContextShift
import cats.implicits._
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetProductsByStore
import AuthenticateJWTOnRequest.AuthenticateJWTOnRequestOps
import ProcessPolymorphicType.ProcessPolymorphicTypeOps

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
      GetProductsByStore:
      ProcessPolymorphicType
  ]: HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "product" / "add" / Produce(produce) / Price(price) / Currency(currency) => {
        (for {
          email <- req.authJWT
          _ <- implicitly[AddProductToStoreForSeller[F]].add(email, produce, Amount(price, currency))
        } yield ResponseBody("Product was added to the store", true))
          .asJsonF        
      }
      case req @ GET -> Root / "product" => {
        for {
          email <- req.authJWT
          response <- implicitly[GetProductsSoldFromSeller[F]].get(email).map(ListOfProduce(_)).asJsonF
        } yield response
      }
      case GET -> Root / "product" / Email(email) => implicitly[GetProductsSoldFromSeller[F]].get(email).map(ListOfProduce(_)).asJsonF     
    }
  }
}
