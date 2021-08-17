package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._

import cats.effect.IO
import com.gardenShare.gardenshare.StoreOrderRequest
import cats.effect.ContextShift
import cats.implicits._
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.StoreOrderRequestWithId
import com.gardenShare.gardenshare.ProductAndQuantity
import slick.jdbc.PostgresProfile
import scala.concurrent.ExecutionContext

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
  def search(id: OrderId)(implicit cs: ContextShift[F]): F[Option[StoreOrderRequestWithId]]
}

object SearchStoreOrderRequestTable {
  implicit def createIOSearchStoreOrderRequestTable(
    implicit gpid: GetProductById[IO],
    client: PostgresProfile.backend.DatabaseDef
  ) = new SearchStoreOrderRequestTable[IO] {
    def search(id: OrderId)(implicit cs: ContextShift[IO]): IO[Option[StoreOrderRequestWithId]] = GetStoreOrderRequestHelper.getStoreOrderWithId(id)
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
      client: PostgresProfile.backend.DatabaseDef
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
            ZoneDateTimeValue.unapply(f._4).flatMap{zdt =>
              Email.unapply(f._3).flatMap{buyerEmail =>
                Email.unapply(f._2).map{sellerEmail =>
                  StoreOrderRequestWithId(f._1, StoreOrderRequest(sellerEmail, buyerEmail, pd.map(ac => ProductAndQuantity(ac._1, ac._2)).toList, zdt.zoneDateTime))
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
        case Some(a) => a
      })
  }
  
  def getStoreOrderWithEmail(e: Email, ge:com.gardenShare.gardenshare.StoreOrderRequestTable.StoreOrderRequestTable => Rep[String])
    (implicit cs: ContextShift[IO],
      g: GetProductById[IO],
      client: PostgresProfile.backend.DatabaseDef
    ): IO[List[StoreOrderRequestWithId]] = {
    val query = for {
        re <- StoreOrderRequestTable.storeOrderRequests if ge(re) === e.underlying.value
      } yield re
    getStoreOrdersWithOrderRequestQuery(query)      
  }

  def getStoreOrderWithId(id: OrderId)(
    implicit cs: ContextShift[IO],
    g: GetProductById[IO],
    client: PostgresProfile.backend.DatabaseDef
  ) = {
    val query = for {
      re <- StoreOrderRequestTable.storeOrderRequests if re.storeRequestId === id.id
    } yield re
    getStoreOrdersWithOrderRequestQuery(query).map(_.headOption)
  }

}
import GetStoreOrderRequestHelper._

abstract class GetStoreOrderRequestsWithSellerEmail[F[_]] {
  def getWithEmail(e: Email)(implicit cs: ContextShift[F]): F[List[StoreOrderRequestWithId]]
}

object GetStoreOrderRequestsWithSellerEmail {
  def createIOGetStoreOrderRequestsWithSellerEmail(
    implicit g: GetProductById[IO],
    client: PostgresProfile.backend.DatabaseDef
  ) = new GetStoreOrderRequestsWithSellerEmail[IO] {
    def getWithEmail(e: Email)(implicit cs: ContextShift[IO]): IO[List[StoreOrderRequestWithId]] = getStoreOrderWithEmail(e, (x: StoreOrderRequestTable.StoreOrderRequestTable) => x.sellerEmail)
  }

  implicit def createIOGetStoreOrderRequestsWithSellerEmail_(
    implicit g: GetProductById[IO],
    client: PostgresProfile.backend.DatabaseDef
  ) = new GetStoreOrderRequestsWithSellerEmail[IO] {
    def getWithEmail(e: Email)(implicit cs: ContextShift[IO]): IO[List[StoreOrderRequestWithId]] = {
      val abc = for {
        resultsWithSellerEmail <- StoreOrderRequestTable.storeOrderRequests join ProductReferenceTable.productReferenceTable on (_.storeRequestId === _.productReferenceTableId) if resultsWithSellerEmail._1.sellerEmail === e.underlying.value
      } yield resultsWithSellerEmail
      for {
        queryResult <- IO.fromFuture(IO(client.run(abc.result)))
        collectionOfOrders = queryResult.groupBy(_._1._1).values.toList
        orders <- collectionOfOrders.map{orderAndReference: Seq[(StoreOrderRequestTableSchema, ProductReferenceTableSchema)] =>
          orderAndReference.headOption.map{headOrder =>
            (Email.unapply(headOrder._1._2), Email.unapply(headOrder._1._3), ZoneDateTimeValue.unapply(headOrder._1._4)) match {
              case (Some(sellerEmail), Some(buyerEmail), Some(dateSubmited)) => {
                orderAndReference.map{y =>
                  g.get(y._2._2).map{ab => ab.map{productWithID =>
                    ProductAndQuantity(productWithID, y._2._3)
                  }}
                }.parSequence
                  .map(acb => acb.collect{
                    case Some(a) => a
                  })
                  .map{(products: Seq[ProductAndQuantity]) =>
                    StoreOrderRequestWithId(headOrder._1._1, StoreOrderRequest(sellerEmail, buyerEmail, products.toList, dateSubmited.zoneDateTime))
                  }
              }
              case (None, _, _) => IO.raiseError(new Throwable(s"Seller email is invalid"))
              case (_, None, _) => IO.raiseError(new Throwable(s"Buyer email is invalid"))
              case (_, _, None) => IO.raiseError(new Throwable(s"Date is invalid"))
            }
          }
        }.collect{
          case Some(a) => a
        }.parSequence
      } yield orders
    }
  }
}

abstract class GetStoreOrderRequestsWithBuyerEmail[F[_]] {
  def getWithEmail(e: Email)(implicit cs: ContextShift[F]): F[List[StoreOrderRequestWithId]]
}

object GetStoreOrderRequestsWithBuyerEmail {
  implicit def createGetStoreOrderRequestsWithBuyerEmailIO(
    implicit client: PostgresProfile.backend.DatabaseDef
  ) = new GetStoreOrderRequestsWithBuyerEmail[IO] {
    def getWithEmail(e: Email)(implicit cs: ContextShift[IO]): IO[List[StoreOrderRequestWithId]] = {
      getStoreOrderWithEmail(e, (x: StoreOrderRequestTable.StoreOrderRequestTable) => x.buyerEmail)
    }
  }
}

