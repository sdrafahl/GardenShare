package com.gardenShare.gardenshare

import cats.effect.IO
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetProductsByStore
import cats.implicits._

abstract class GetProductsSoldFromSeller[F[_]] {
  def get(email: Email)(implicit g:GetStore[F], b: GetProductsByStore[F]): F[List[ProductWithId]]
}

object GetProductsSoldFromSeller {
  implicit object IOGetProductsSoldFromSeller extends GetProductsSoldFromSeller[IO] {
    def get(email: Email)(
      implicit g:GetStore[IO],
      b:GetProductsByStore[IO]): IO[List[ProductWithId]] = {
      for {
        stores <- g.getStoresByUserEmail(email)
        listOfList <- stores.map(store => b.getProductsByStore(store.id)).sequence
        listOfProducts = listOfList.flatten        
      } yield listOfProducts      
    }
  }
}
