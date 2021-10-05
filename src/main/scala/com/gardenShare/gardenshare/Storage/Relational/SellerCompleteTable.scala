package com.gardenShare.gardenshare

import cats.effect.IO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile

object SellerCompleteTableSchemas {
  type CompleteTableScheme = (OrderId)
}
object SellerCompleteTable {
  class SellerCompleteTable(tag: Tag) extends Table[SellerCompleteTableSchemas.CompleteTableScheme](tag, "sellercompletetable") {
    def orderId = column[OrderId]("orderId")
    def * = (orderId)
  }
  val sellerCompleteTable = TableQuery[SellerCompleteTable]
}

abstract class InsertOrderIntoCompleteTable[F[_]] {
  def insertOrder(orderId: OrderId): F[Unit]
}

object InsertOrderIntoCompleteTable {
  implicit def createIOInsertOrderIntoCompleteTable(
    implicit client: PostgresProfile.backend.DatabaseDef,
  ) = new InsertOrderIntoCompleteTable[IO] {
    def insertOrder(orderId: OrderId): IO[Unit] = {
      val table = SellerCompleteTable.sellerCompleteTable
      val query = table += orderId
      IO.fromFuture(IO(client.run(query))) *> (IO.unit)
    }
  }
}

abstract class SellerCompleteOrders[F[_]] {
  def search(orderId: OrderId): F[Option[SellerCompleteTableSchemas.CompleteTableScheme]] 
}

object SellerCompleteOrders {
  implicit def createIOSellerCompleteOrders(
    implicit client: PostgresProfile.backend.DatabaseDef,
  ) = new SellerCompleteOrders[IO] {
    def search(orderId: OrderId): IO[Option[SellerCompleteTableSchemas.CompleteTableScheme]] = {
      val query = for {
        results <- SellerCompleteTable.sellerCompleteTable if results.orderId === orderId
      } yield (results)      
      IO.fromFuture(IO(client.run(query.result))).map(_.headOption)
    }
  }
}
