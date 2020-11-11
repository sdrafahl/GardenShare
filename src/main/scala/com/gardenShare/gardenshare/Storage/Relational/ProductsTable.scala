package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
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
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import scala.util.Success
import scala.util.Failure

object ProductTable {
  class ProductTable(tag: Tag) extends Table[(Int, Int, Int, String)](tag, "products") {
    def productId = column[Int]("productId", O.PrimaryKey, O.AutoInc)
    def storeId = column[Int]("storeId")
    def orderId = column[Int]("orderId")
    def s3productInfo = column[String]("s3productInfo")
    def * = (productId, storeId, orderId, s3productInfo)    
  }
  val products = TableQuery[ProductTable]
}

abstract class GetProductByID[F[_]] {
  def getProduct(id: Int): F[Option[Product]]
}

object GetProductByID {
  def apply[F[_]: GetProductByID]() = implicitly[GetProductByID[F]]
  implicit object IOGetProductByID extends GetProductByID[IO] {
    def getProduct(id: Int): IO[Option[Product]] = {
      val query = for {
        products <- ProductTable.products if products.productId equals id
      } yield (products.productId, products.storeId, products.orderId, products.s3productInfo)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(_.map(ac => Product(ac._1, ac._2, ac._3, DescriptionAddress(ac._4))))
        .map(_.headOption)
    }
  }
}

abstract class GetProductsByStore[F[_]: Async] {
  def getProductsByStore(storeid: Int): F[List[Product]]
}

object GetProductsByStore {
  def apply[F[_]: GetProductsByStore]() = implicitly[GetProductsByStore[F]]

  implicit object IOGetProductsByStore extends GetProductsByStore[IO]{
    def getProductsByStore(storeid: Int): IO[List[Product]] = {
      val query = for {
        products <- ProductTable.products if products.storeId equals storeid
      } yield (products.productId, products.storeId, products.orderId, products.s3productInfo)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(lst => lst.map(
          f => Product(f._1, f._2, f._3, DescriptionAddress(f._4))
        ))
    }
  }
}

abstract class InsertProduct[F[_]] {
  def add(data: List[CreateProductRequest]): F[List[Product]]
}

object InsertProduct {
  def apply[F[_]: InsertProduct]() = implicitly[InsertProduct[F]]

  implicit object IOInsertProduct extends InsertProduct[IO] {
    def add(data: List[CreateProductRequest]): IO[List[Product]] = {
      val query = ProductTable.products
      val qu = ProductTable.products.returning(query)
      val res = qu ++= data.map(da => (0, da.storeId, da.orderId, da.descriptionAddresss.address))
      val responses = IO.fromFuture(IO(Setup.db.run(res))).map(_.toList)
      responses.map(res => res.map{ resp =>
        Product(resp._1, resp._2, resp._3, DescriptionAddress(resp._4))
      })
    }
  }
}

abstract class AddOrderIdToProduct[F[_]] {
  def add(orderId: Int, productId: Int): F[Product]
}

object AddOrderIdToProduct {
  def apply[F[_]: AddOrderIdToProduct]() = implicitly[AddOrderIdToProduct[F]]
  implicit object IOAddOrderIdToProduct extends AddOrderIdToProduct[IO] {
    def add(orderId: Int, productId: Int): IO[Product] = {
      val query = for {
        prods <- ProductTable.products if prods.productId equals productId
      } yield (prods.productId, prods.storeId, prods.orderId, prods.s3productInfo)

      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(_.map(f => Product(f._1, f._2, f._3, DescriptionAddress(f._4))))
        .map(_.head)
        .flatMap {prod =>
          val updateQuery = ProductTable
            .products
            .filter(_.productId === productId)
            .map(or => (or.productId, or.storeId, or.orderId, or.s3productInfo))
            .update((prod.id, prod.storeId, orderId, prod.descriptionS3Address.address))
            .asTry

          IO.fromFuture(IO(Setup.db.run(updateQuery)))
            .map(c => c.map(_ => Product(prod.id, prod.storeId, orderId, prod.descriptionS3Address)))
            .flatMap {
              case Success(order) => IO(order)
              case Failure(err) => IO.raiseError(err)
            }           
        }
    }
  }
}
