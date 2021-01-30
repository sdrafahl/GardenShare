package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.domain.Store.Store
import com.gardenShare.gardenshare.Storage.Relational.InsertProduct.InsertProductOps
import com.gardenShare.gardenshare.Storage.Relational.InsertProduct
import cats.effect.IO

abstract class AddProductToStore[F[_]] {
  def add(s: Store, pd: Produce): F[Unit]
}

object AddProductToStore {
  implicit def createIOAddProductToStore(implicit i: InsertProduct[IO]): AddProductToStore[IO] = new AddProductToStore[IO]{
    def add(s: Store, pd: Produce): IO[Unit] = i.add(List(CreateProductRequest(s.id, pd)))
  }
}
