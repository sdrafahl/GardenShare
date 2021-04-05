package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Store._
import com.gardenShare.gardenshare.Sequence.SequenceEithers
import com.gardenShare.gardenshare.Sequence.SequenceEithers._
import scala.util.Success
import scala.util.Failure
import com.gardenShare.gardenshare.GetproductDescription.GetproductDescriptionOps
import com.gardenShare.gardenshare._
import slick.jdbc.PostgresProfile

object ProductTable {
  class ProductTable(tag: Tag) extends Table[(Int, Int, String, Int, String)](tag, "products") {
    def productId = column[Int]("productId", O.PrimaryKey, O.AutoInc)
    def storeId = column[Int]("storeId")
    def productName = column[String]("productName")
    def productPrice = column[Int]("productPrice")
    def productPriceType = column[String]("productPriceType")
    def * = (productId, storeId, productName, productPrice, productPriceType)    
  }
  val products = TableQuery[ProductTable]
}

abstract class GetProductById[F[_]] {
  def get(id: Int)(implicit cs: ContextShift[F]): F[Option[ProductWithId]]
}

object GetProductById {
  implicit def createIOGetProductById(implicit e:Parser[Currency], pp: Parser[Produce], client: PostgresProfile.backend.DatabaseDef) = new GetProductById[IO] {
    def get(id: Int)(
      implicit cs: ContextShift[IO]      
    ): IO[Option[ProductWithId]] = {
      val query = for {
        re <- ProductTable.products if re.productId === id
      } yield re

      IO.fromFuture(IO(client.run(query.result)))
        .map(_.headOption)
        .map(_.flatMap{f =>
          pp.parse(f._3).flatMap{produce =>
            e.parse(f._5).map{currencyType =>
              ProductWithId(f._1, Product(f._2, produce, Amount(f._4, currencyType)))
            }            
          }.toOption          
        })
    }
  }
}

abstract class GetProductsByStore[F[_]: Async] {
  def getProductsByStore(storeid: Int)(implicit cs:ContextShift[F]): F[List[ProductWithId]]
}

object GetProductsByStore {
  def apply[F[_]: GetProductsByStore]() = implicitly[GetProductsByStore[F]]

  implicit def IOGetProductsByStore(implicit e:Parser[Currency], pp: Parser[Produce], client: PostgresProfile.backend.DatabaseDef) = new GetProductsByStore[IO]{
    def getProductsByStore(storeid: Int)(
      implicit cs:ContextShift[IO]      
    ): IO[List[ProductWithId]] = {
      val query = for {
        products <- ProductTable.products if products.storeId === storeid
      } yield (products.productId, products.storeId, products.productName, products.productPrice, products.productPriceType)
      IO.fromFuture(IO(client.run(query.result)))
        .map(_.toList)
        .map(_.map(xy => (xy._1, xy._2, xy._3, xy._4, e.parse(xy._5))))
        .map(lst => (
          lst.collect{
            case (a, b, c, d, Right(e)) => (a, b, c, d, e)
          }.map(
            f => {
              pp.parse(f._3).map{prd =>
                ProductWithId(f._1, Product(f._2, prd, Amount(f._4, f._5)))
              }              
            }
          )
        )).map(f => f.collect{
          case Right(a) => a
        })
    }
  }
}


abstract class InsertProduct[F[_]] {
  def add(l: List[CreateProductRequest])(implicit cs: ContextShift[F]): F[Unit]
}

object InsertProduct {
  def apply[F[_]: InsertProduct]() = implicitly[InsertProduct[F]]

  implicit def IOInsertProduct(implicit g:GetproductDescription[Produce], et: EncodeToString[Currency], client: PostgresProfile.backend.DatabaseDef) = new InsertProduct[IO] {
    def add(l: List[CreateProductRequest])(
      implicit cs: ContextShift[IO]      
    ): IO[Unit] = {
      val table = ProductTable.products
      val qu = ProductTable.products.returning(table)
      val res = (qu ++= l.map(da => (0, da.storeId, g.gestDesc(da.product).name, da.am.quantityOfCurrency, et.encode(da.am.currencyType)))).transactionally

      IO.fromFuture(IO(client.run(res))).flatMap(_ => IO.unit)
    }
  }
  implicit class InsertProductOps(underlying: List[CreateProductRequest]) {
    def addProduct[F[_]: InsertProduct:ContextShift] = implicitly[InsertProduct[F]].add(underlying)
  }
}
