package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.AuthJWT
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import com.gardenShare.gardenshare.Email
import cats.implicits._
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetProductsByStore
import ProcessPolymorphicType.ProcessPolymorphicTypeOps
import org.http4s.AuthedRoutes
import org.http4s.server.AuthMiddleware

object ProductRoutes {
  def productRoutes[F[_]:
      Async:
      AuthJWT:
      AddProductToStoreForSeller:
      GetUserInfo:
      AddProductToStore:
      GetProductsSoldFromSeller:
      GetStore:
      GetProductsByStore:
      ProcessPolymorphicType
  ](implicit authMiddleWear: AuthMiddleware[F, Email]): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._

    val authedRoutes: AuthedRoutes[Email, F] =
      AuthedRoutes.of {

        case POST -> Root / "product" / "add" / Produce(produce) / Price(price) / Currency(currency) as email => (for {
          _ <- implicitly[AddProductToStoreForSeller[F]].add(email, produce, Amount(price, currency))
        } yield ResponseBody("Product was added to the store", true)).asJsonF

        case GET -> Root / "product" as email => for {
          response <- implicitly[GetProductsSoldFromSeller[F]].get(email).map(ListOfProduce(_)).asJsonF
        } yield response
      }

    val nonauthedRoutes = HttpRoutes.of[F] {      
      case GET -> Root / "product" / Email(email) => implicitly[GetProductsSoldFromSeller[F]].get(email).map(ListOfProduce(_)).asJsonF     
    }


    nonauthedRoutes <+> authMiddleWear(authedRoutes)
  }
}
