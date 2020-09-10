package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.domain.Store._
import com.gardenShare.gardenshare.domain.Products._
import com.gardenShare.gardenshare.domain.Products.ParseDescriptionAddress._
import com.gardenShare.gardenshare.Sequence.SequenceEithers
import com.gardenShare.gardenshare.Sequence.SequenceEithers._
import com.gardenShare.gardenshare.domain.Products.ParseDescriptionAddress.Ops

object ProductTable {
  class ProductTable(tag: Tag) extends Table[(Int, Int, String)](tag, "products") {
    def productId = column[Int]("productId", O.PrimaryKey, O.AutoInc)
    def storeId = column[Int]("storeId")
    def s3productInfo = column[String]("storeId")
    def * = (productId, storeId, s3productInfo)    
  }
  val products = TableQuery[ProductTable]
}

abstract class GetProductsByStore[F[_]: Async] {
  def getProductsByStore(store: Store): F[List[Product]]
}

object GetProductsByStore {
  def apply[F[_]: GetProductsByStore]() = implicitly[GetProductsByStore[F]]

  implicit object IOGetProductsByStore extends GetProductsByStore[IO]{
    def getProductsByStore(store: Store): IO[List[Product]] = {
      val query = for {
        products <- ProductTable.products if products.storeId equals store.id
      } yield (products.productId, products.storeId, products.s3productInfo)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(lst => lst.map(
          f => Product(f._1, f._2, DescriptionAddress(f._3))
        ))
    }
  }
}

abstract class InsertProduct[F[_]] {
  def add(data: List[CreateProductRequest]): F[List[Product]]
}

object InsertProduct {
  def apply[F[_]: InsertProduct] = implicitly[InsertProduct[F]]

  implicit object IOInsertProduct extends InsertProduct[IO] {
    def add(data: List[CreateProductRequest]): IO[List[Product]] = {
      val query = ProductTable.products
      val qu = ProductTable.products.returning(query)
      val res = qu ++= data.map(da => (0, da.storeId, da.descriptionAddresss.address))
      val responses = IO.fromFuture(IO(Setup.db.run(res))).map(_.toList)
      responses.map(res => res.map{ resp =>
        Product(resp._1, resp._2, DescriptionAddress(resp._3))
      })
    }
  }
}
