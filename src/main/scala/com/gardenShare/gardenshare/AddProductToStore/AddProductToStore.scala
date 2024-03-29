package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Store
import com.gardenShare.gardenshare.InsertProduct
import cats.effect.IO
import com.gardenShare.gardenshare.GetProductsByStore

abstract class AddProductToStore[F[_]] {
  def add(s: Store, pd: Produce, am: Amount): F[Unit]
}

object AddProductToStore {
  implicit def createIOAddProductToStore(implicit i: InsertProduct[IO], g:GetProductsByStore[IO]): AddProductToStore[IO] = new AddProductToStore[IO]{
    def add(s: Store, pd: Produce, am: Amount): IO[Unit] = {
      for {
        produceList <- g.getProductsByStore(s.id)
        x <- if(!produceList.contains(pd)) {
          i.add(List(CreateProductRequest(s.id, pd, am)))
        } else {
          IO.unit
        }
      } yield x
    }
  }
}
