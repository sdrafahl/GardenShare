package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetProductsByStore
import cats.effect.ContextShift

abstract class GetProductsSoldFromSeller[F[_]] {
  def get(email: Email)(implicit g:GetStore[F], b: GetProductsByStore[F], c:Parser[Produce], cs: ContextShift[F]): F[List[ProductWithId]]
}

object GetProductsSoldFromSeller {
  implicit object IOGetProductsSoldFromSeller extends GetProductsSoldFromSeller[IO] {
    def get(email: Email)(
      implicit g:GetStore[IO],
      b:GetProductsByStore[IO],
      c:Parser[Produce],
      cs: ContextShift[IO]
    ): IO[List[ProductWithId]] = {
      g.getStoresByUserEmail(email).map(_.headOption).flatMap{maybeSomeStore =>
        maybeSomeStore.map{store =>
          b.getProductsByStore(store.id)
        }.getOrElse(IO.pure(List()))
      }
    }
  }
}
