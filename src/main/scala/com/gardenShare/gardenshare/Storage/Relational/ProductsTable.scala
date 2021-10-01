package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import cats.effect._
import com.gardenShare.gardenshare._
import slick.jdbc.PostgresProfile
import cats.implicits._

object ProductTable {
  class ProductTable(tag: Tag) extends Table[(ProductId, Int, String, Int, String)](tag, "products") {
    def productId = column[ProductId]("productId", O.PrimaryKey, O.AutoInc)
    def storeId = column[Int]("storeId")
    def productName = column[String]("productName")
    def productPrice = column[Int]("productPrice")
    def productPriceType = column[String]("productPriceType")
    def * = (productId, storeId, productName, productPrice, productPriceType)    
  }
  val products = TableQuery[ProductTable]
}

abstract class GetProductById[F[_]] {
  def get(id: ProductId): F[Option[ProductWithId]]
}

object GetProductById {
  implicit def createIOGetProductById(implicit client: PostgresProfile.backend.DatabaseDef) = new GetProductById[IO] {
    def get(id: ProductId): IO[Option[ProductWithId]] = {
      val query = for {
        re <- ProductTable.products if re.productId === id
      } yield re

      IO.fromFuture(IO(client.run(query.result)))
        .map(_.headOption)
        .map(_.flatMap{f =>          
          Produce.unapply(f._3).flatMap{produce =>
            Currency.unapply(f._5).map{currencyType =>
              ProductWithId(f._1, Product(f._2, produce, Amount(Price(f._4), currencyType)))
            }            
          }
        })
    }
  }
}

abstract class GetProductsByStore[F[_]] {
  def getProductsByStore(storeid: Int): F[List[ProductWithId]]
}

object GetProductsByStore {
  def apply[F[_]: GetProductsByStore]() = implicitly[GetProductsByStore[F]]

  implicit def IOGetProductsByStore(implicit client: PostgresProfile.backend.DatabaseDef) = new GetProductsByStore[IO]{
    def getProductsByStore(storeid: Int): IO[List[ProductWithId]] = {
      val query = for {
        products <- ProductTable.products if products.storeId === storeid
      } yield (products.productId, products.storeId, products.productName, products.productPrice, products.productPriceType)
      IO.fromFuture(IO(client.run(query.result)))
        .map(_.toList)
        .map(_.map(xy => (xy._1, xy._2, xy._3, xy._4, Currency.unapply(xy._5))))
        .map(lst => (
          lst.collect{
            case (a, b, c, d, Some(e)) => (a, b, c, d, e)
          }.map(
            f => {
              Produce.unapply(f._3).map{prd =>
                ProductWithId(f._1, Product(f._2, prd, Amount(Price(f._4), f._5)))
              }              
            }
          )
        )).map(f => f.collect{
          case Some(a) => a
        })      
    }
  }
}


abstract class InsertProduct[F[_]] {
  def add(l: List[CreateProductRequest]): F[Unit]
}

object InsertProduct {
  def apply[F[_]: InsertProduct]() = implicitly[InsertProduct[F]]

  implicit def IOInsertProduct(implicit g:GetProduceDescription[IO], client: PostgresProfile.backend.DatabaseDef) = new InsertProduct[IO] {
    def add(l: List[CreateProductRequest]): IO[Unit] = {
      val table = ProductTable.products
      val qu = ProductTable.products.returning(table)
      for {
        names <- l.map(da => g.get(da.product)).sequence
        creatProductRequestsWithProductDescriptions = l.zip(names)
        res = (qu ++= creatProductRequestsWithProductDescriptions.map(da => (ProductId(0), da._1.storeId, da._2.name, da._1.am.quantityOfCurrency.value, da._1.am.currencyType.show))).transactionally
        _ <- IO.fromFuture(IO(client.run(res))).flatMap(_ => IO.unit)
      }  yield ()      
    }
  }
  implicit class InsertProductOps(underlying: List[CreateProductRequest]) {
    def addProduct[F[_]: InsertProduct] = implicitly[InsertProduct[F]].add(underlying)
  }
}
