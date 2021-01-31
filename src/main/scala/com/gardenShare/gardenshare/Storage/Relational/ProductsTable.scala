package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.domain.Store._
import com.gardenShare.gardenshare.Sequence.SequenceEithers
import com.gardenShare.gardenshare.Sequence.SequenceEithers._
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import scala.util.Success
import scala.util.Failure
import com.gardenShare.gardenshare.GetproductDescription.GetproductDescriptionOps
import com.gardenShare.gardenshare._

object ProductTable {
  class ProductTable(tag: Tag) extends Table[(Int, Int, String)](tag, "products") {
    def productId = column[Int]("productId", O.PrimaryKey, O.AutoInc)
    def storeId = column[Int]("storeId")
    def productName = column[String]("productName")
    def * = (productId, storeId, productName)    
  }
  val products = TableQuery[ProductTable]
}

// abstract class GetProductByID[F[_]] {
//   def getProduct(id: Int): F[Option[Product]]
// }

// object GetProductByID {
//   def apply[F[_]: GetProductByID]() = implicitly[GetProductByID[F]]
//   implicit object IOGetProductByID extends GetProductByID[IO] {
//     def getProduct(id: Int): IO[Option[Product]] = {
//       val query = for {
//         products <- ProductTable.products if products.productId === id
//       } yield (products.productId, products.storeId, products.productName)
//       IO.fromFuture(IO(Setup.db.run(query.result)))
//         .map(_.toList)
//         .map(_.map(ac => Product(ac._1, ac._2, ac._3, DescriptionAddress(ac._4))))
//         .map(_.headOption)
//     }
//   }
// }

abstract class GetProductsByStore[F[_]: Async] {
  def getProductsByStore(storeid: Int): F[List[Product]]
}

object GetProductsByStore {
  def apply[F[_]: GetProductsByStore]() = implicitly[GetProductsByStore[F]]

  implicit object IOGetProductsByStore extends GetProductsByStore[IO]{
    def getProductsByStore(storeid: Int): IO[List[Product]] = {
      val query = for {
        products <- ProductTable.products if products.storeId === storeid
      } yield (products.productId, products.storeId, products.productName)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(lst => lst.map(
          f => Product(f._1, f._2, f._3)
        ))
    }
  }
}


abstract class InsertProduct[F[_]] {
  def add(l: List[CreateProductRequest]): F[Unit]
}

object InsertProduct {
  def apply[F[_]: InsertProduct]() = implicitly[InsertProduct[F]]

  implicit def IOInsertProduct(implicit g:GetproductDescription[Produce]) = new InsertProduct[IO] {
    def add(l: List[CreateProductRequest]): IO[Unit] = {
      val table = ProductTable.products
      val qu = ProductTable.products.returning(table)
      val res = qu ++= l.map(da => (0, da.storeId, g.gestDesc(da.product).name))
      IO.fromFuture(IO(Setup.db.run(res))).flatMap(_ => IO.unit)
    }
  }
  implicit class InsertProductOps(underlying: List[CreateProductRequest]) {
    def addProduct[F[_]: InsertProduct] = implicitly[InsertProduct[F]].add(underlying)
  }
}
