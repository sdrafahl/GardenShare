package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities._
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import com.gardenShare.gardenshare.Storage.Relational.GetProductsByStore

abstract class GetProductsSoldFromSeller[F[_]] {
  def get(email: Email)(implicit g:GetStore[F], b: GetProductsByStore[F], c:ParseProduce[String]): F[List[Produce]]
}

object GetProductsSoldFromSeller {
  implicit object IOGetProductsSoldFromSeller extends GetProductsSoldFromSeller[IO] {
    def get(email: Email)(implicit g:GetStore[IO], b:GetProductsByStore[IO], c:ParseProduce[String]): IO[List[Produce]] = {
      g.getStoresByUserEmail(email).map(_.headOption).flatMap{maybeSomeStore =>
        maybeSomeStore.map{store =>
          b.getProductsByStore(store.id).map{pds =>
            pds
              .map(prod => c.parse(prod.productName))
              .collect{
                case Right(x) => x
              }
          }
        }.getOrElse(IO.pure(List()))
      }
    }
  }
}
