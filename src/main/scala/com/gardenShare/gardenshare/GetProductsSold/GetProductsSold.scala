package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities._
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import com.gardenShare.gardenshare.Storage.Relational.GetProductsByStore

abstract class GetProductsSoldFromSeller[F[_]] {
  def get(email: Email)(implicit g:GetStore[F], b: GetProductsByStore[F], c:ParseProduce[String]): F[List[ProductWithId]]
}

object GetProductsSoldFromSeller {
  implicit object IOGetProductsSoldFromSeller extends GetProductsSoldFromSeller[IO] {
    def get(email: Email)(implicit g:GetStore[IO], b:GetProductsByStore[IO], c:ParseProduce[String]): IO[List[ProductWithId]] = {
      g.getStoresByUserEmail(email).map(_.headOption).flatMap{maybeSomeStore =>
        maybeSomeStore.map{store =>
          b.getProductsByStore(store.id)
        }.getOrElse(IO.pure(List()))
      }
    }
  }
}
