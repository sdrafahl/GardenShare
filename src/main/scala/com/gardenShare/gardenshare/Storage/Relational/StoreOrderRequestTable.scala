package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIOAction
import slick.lifted.AbstractTable

import cats.effect.IO
import com.gardenShare.gardenshare.StoreOrderRequest
import cats.effect.ContextShift
import cats.implicits._
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.StoreOrderRequestWithId
import java.time.ZonedDateTime
import scala.util.Try
import com.gardenShare.gardenshare.ParseBase64EncodedZoneDateTime
import com.gardenShare.gardenshare.ProductAndQuantity
import slick.jdbc.PostgresProfile
import scala.concurrent.ExecutionContext
import java.time.ZonedDateTime

object StoreOrderRequestTableSchemas {
  type StoreOrderRequestTableSchema = (Int, String, String, String)
  type ProductReferenceTableSchema = (Int, Int, Int)
}
import StoreOrderRequestTableSchemas._

object StoreOrderRequestTable {
  class StoreOrderRequestTable(tag: Tag) extends Table[StoreOrderRequestTableSchema](tag, "storeorderrequest") {
    def storeRequestId = column[Int]("storeorderrequestid", O.PrimaryKey, O.AutoInc)
    def sellerEmail = column[String]("selleremail")
    def buyerEmail = column[String]("buyeremail")
    def datesubmited = column[String]("datesubmited")    
    def *  = (storeRequestId, sellerEmail, buyerEmail, datesubmited)
  }
  val storeOrderRequests = TableQuery[StoreOrderRequestTable]
}

object ProductReferenceTable {
  class ProductReferenceTable(tag: Tag) extends Table[ProductReferenceTableSchema](tag, "productreferencetable") {
    def productReferenceTableId = column[Int]("productreferencetableid")    
    def productId = column[Int]("productid")
    def productQuantity = column[Int]("quantity")
    def * = (productReferenceTableId, productId, productQuantity)
  }
  val productReferenceTable = TableQuery[ProductReferenceTable]
}

abstract class SearchProductReferences[F[_]] {
  def search(productReferenceTableId: Int)(implicit cs: ContextShift[F]): F[List[ProductReferenceTableSchema]]
}

object SearchProductReferences {
  implicit def createIOInsertProductReferences(implicit client: PostgresProfile.backend.DatabaseDef) = new SearchProductReferences[IO] {
    def search(productReferenceTableId: Int)(implicit cs: ContextShift[IO]): IO[List[ProductReferenceTableSchema]] = {
      val query = for {
        res <- ProductReferenceTable.productReferenceTable if res.productReferenceTableId === productReferenceTableId
      } yield res
      IO.fromFuture(IO(client.run(query.result))).map(_.toList)
    }
  }
}

abstract class SearchStoreOrderRequestTable[F[_]] {
  def search(id: Int)(implicit cs: ContextShift[F]): F[Option[StoreOrderRequestWithId]]
}

object SearchStoreOrderRequestTable {
  implicit def createIOSearchStoreOrderRequestTable(
    implicit searchProductRefs: SearchProductReferences[IO],
    gpid: GetProductById[IO],
    client: PostgresProfile.backend.DatabaseDef,
    emailParser: com.gardenShare.gardenshare.Parser[Email],
    parseDate: com.gardenShare.gardenshare.Parser[ZonedDateTime]
  ) = new SearchStoreOrderRequestTable[IO] {
    def search(id: Int)(implicit cs: ContextShift[IO]): IO[Option[StoreOrderRequestWithId]] = GetStoreOrderRequestHelper.getStoreOrderWithId(id)
  }
}

abstract class DeleteStoreOrderRequestsForSeller[F[_]] {
  def delete(e: Email)(implicit cs: ContextShift[F]): F[Unit]
}

object DeleteStoreOrderRequestsForSeller {
  implicit def createIODeleteStoreOrderRequestsForSeller(implicit getter: GetStoreOrderRequestsWithSellerEmail[IO], client: PostgresProfile.backend.DatabaseDef) = new DeleteStoreOrderRequestsForSeller[IO] {
    def delete(e: Email)(implicit cs: ContextShift[IO]): IO[Unit] = {

      def createDeleteOrdersByIdQuery(id: Int) = {
        for {
          a <- StoreOrderRequestTable.storeOrderRequests if a.storeRequestId === id
        } yield a
      }

      def createQueryFoProductReferences(prodctReferenceTableId: Int) = {
        for {
          a <- ProductReferenceTable.productReferenceTable if a.productReferenceTableId === prodctReferenceTableId
        } yield a
      }

      for {
        orders <- getter.getWithEmail(e)
        _ <- orders.map{order =>
          val pgmToDeleteProductReferences = order.storeOrderRequest.products.map(prod => (IO.fromFuture(IO(client.run(createQueryFoProductReferences(prod.product.id).delete))))).parSequence
          val pgmToDeleteOrderRequests =  IO.fromFuture(IO(client.run(createDeleteOrdersByIdQuery(order.id).delete)))
          pgmToDeleteProductReferences &> pgmToDeleteOrderRequests
        }.parSequence
      } yield ()
    }
  }
}

abstract class InsertStoreOrderRequest[F[_]] {
  def insertStoreOrderRequest(req: StoreOrderRequest)(implicit cs: ContextShift[F], ec: ExecutionContext): F[StoreOrderRequestWithId]
}

object InsertStoreOrderRequest {
  implicit def createIOInsertStoreOrderRequest(implicit client: PostgresProfile.backend.DatabaseDef) = new InsertStoreOrderRequest[IO] {
    def insertStoreOrderRequest(req: StoreOrderRequest)(implicit cs: ContextShift[IO], ec: ExecutionContext): IO[StoreOrderRequestWithId] = {
      val storeOrderRequestTable = StoreOrderRequestTable.storeOrderRequests
      val qu = StoreOrderRequestTable.storeOrderRequests.returning(storeOrderRequestTable)
      val res = qu += (0, req.seller.underlying.value, req.buyer.underlying.value, req.dateSubmitted.toString())

      val prodRefTable = ProductReferenceTable.productReferenceTable
      val prodRefRequest = ProductReferenceTable.productReferenceTable.returning(prodRefTable)
      val query = (for {
        product <- res
        productReferencesToAdd = req.products.map{f => (product._1, f.product.id, f.quantity)}
        
        _ <- prodRefRequest ++= productReferencesToAdd
      } yield product).transactionally
      IO.fromFuture(IO(client.run(query))).map{a => StoreOrderRequestWithId(a._1, req)}      
    }
  }
}

object GetStoreOrderRequestHelper {

  def getStoreOrdersWithOrderRequestQuery(query: Query[com.gardenShare.gardenshare.StoreOrderRequestTable.StoreOrderRequestTable, StoreOrderRequestTableSchema, Seq])
    (implicit cs: ContextShift[IO],
      g: GetProductById[IO],
      zoneDateParser: Parser[ZonedDateTime],
      client: PostgresProfile.backend.DatabaseDef,
      emailParser: com.gardenShare.gardenshare.Parser[Email]
    ) = {
    IO.fromFuture(IO(client.run(query.result))).map(_.map{f =>
      val productReferenceQuery = for {
        pre <- ProductReferenceTable.productReferenceTable if pre.productReferenceTableId === f._1
      } yield pre

      IO.fromFuture(IO(client.run(productReferenceQuery.result))).flatMap{abb =>
        abb.map{a =>
          g.get(a._2).map(lk => (lk, a._3))
        }
          .parSequence
          .map(_.collect{
            case (Some(a), b) => (a, b)
          }).map{pd =>
            zoneDateParser.parse(f._4).flatMap{zdt =>
              emailParser.parse(f._3).flatMap{buyerEmail =>
                emailParser.parse(f._2).map{sellerEmail =>
                  StoreOrderRequestWithId(f._1, StoreOrderRequest(sellerEmail, buyerEmail, pd.map(ac => ProductAndQuantity(ac._1, ac._2)).toList, zdt))
                }                
              }              
            }
          }
      }
    })
      .map(_.parSequence)
      .flatten
      .map(_.toList)
      .map(_.collect{
        case Right(a) => a
      })
  }
  
  def getStoreOrderWithEmail(e: Email, ge:com.gardenShare.gardenshare.StoreOrderRequestTable.StoreOrderRequestTable => Rep[String])
    (implicit cs: ContextShift[IO],
      g: GetProductById[IO],
      par: Parser[ZonedDateTime],
      client: PostgresProfile.backend.DatabaseDef,
      emailParser: com.gardenShare.gardenshare.Parser[Email]
    ): IO[List[StoreOrderRequestWithId]] = {
    val query = for {
        re <- StoreOrderRequestTable.storeOrderRequests if ge(re) === e.underlying.value
      } yield re
    getStoreOrdersWithOrderRequestQuery(query)      
  }

  def getStoreOrderWithId(id: Int)(
    implicit cs: ContextShift[IO],
    g: GetProductById[IO],
    dateParser: Parser[ZonedDateTime],
    client: PostgresProfile.backend.DatabaseDef,
    emailParser: com.gardenShare.gardenshare.Parser[Email]
  ) = {
    val query = for {
      re <- StoreOrderRequestTable.storeOrderRequests if re.storeRequestId === id
    } yield re
    getStoreOrdersWithOrderRequestQuery(query).map(_.headOption)
  }

}
import GetStoreOrderRequestHelper._

abstract class GetStoreOrderRequestsWithSellerEmail[F[_]] {
  def getWithEmail(e: Email)(implicit cs: ContextShift[F]): F[List[StoreOrderRequestWithId]]
}

object GetStoreOrderRequestsWithSellerEmail {
  implicit def createIOGetStoreOrderRequestsWithSellerEmail(
    implicit g: GetProductById[IO],
    client: PostgresProfile.backend.DatabaseDef,
    emailParser: com.gardenShare.gardenshare.Parser[Email],
    timeParser: Parser[ZonedDateTime]
  ) = new GetStoreOrderRequestsWithSellerEmail[IO] {
    def getWithEmail(e: Email)(implicit cs: ContextShift[IO]): IO[List[StoreOrderRequestWithId]] = getStoreOrderWithEmail(e, (x: StoreOrderRequestTable.StoreOrderRequestTable) => x.sellerEmail)
  }
}

abstract class GetStoreOrderRequestsWithBuyerEmail[F[_]] {
  def getWithEmail(e: Email)(implicit cs: ContextShift[F]): F[List[StoreOrderRequestWithId]]
}

object GetStoreOrderRequestsWithBuyerEmail {
  implicit def createGetStoreOrderRequestsWithBuyerEmailIO(
    implicit client: PostgresProfile.backend.DatabaseDef,
    emailParser: com.gardenShare.gardenshare.Parser[Email],
    dateParser: Parser[ZonedDateTime]
  ) = new GetStoreOrderRequestsWithBuyerEmail[IO] {
    def getWithEmail(e: Email)(implicit cs: ContextShift[IO]): IO[List[StoreOrderRequestWithId]] = {
      getStoreOrderWithEmail(e, (x: StoreOrderRequestTable.StoreOrderRequestTable) => x.buyerEmail)
    }
  }
}

