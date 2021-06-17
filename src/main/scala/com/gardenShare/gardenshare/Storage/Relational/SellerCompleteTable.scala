package com.gardenShare.gardenshare

import cats.effect.IO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile
import cats.effect.ContextShift

object SellerCompleteTableSchemas {
  type CompleteTableScheme = (Int)
}
object SellerCompleteTable {
  class SellerCompleteTable(tag: Tag) extends Table[SellerCompleteTableSchemas.CompleteTableScheme](tag, "sellercompletetable") {
    def orderId = column[Int]("orderId")
    def * = (orderId)
  }
  val sellerCompleteTable = TableQuery[SellerCompleteTable]
}

abstract class InsertOrderIntoCompleteTable[F[_]] {
  def insertOrder(orderId: Int)(implicit cs: ContextShift[F]): F[Unit]
}

object InsertOrderIntoCompleteTable {
  implicit def createIOInsertOrderIntoCompleteTable(
    implicit client: PostgresProfile.backend.DatabaseDef,
  ) = new InsertOrderIntoCompleteTable[IO] {
    def insertOrder(orderId: Int)(implicit cs: ContextShift[IO]): IO[Unit] = {
      val table = SellerCompleteTable.sellerCompleteTable
      val query = table += orderId
      IO.fromFuture(IO(client.run(query))) *> (IO.unit)
    }
  }
}

abstract class SearchCompletedOrders[F[_]] {
  def search(orderId: Int)(implicit cs: ContextShift[F]): F[Option[SellerCompleteTableSchemas.CompleteTableScheme]] 
}

object SearchCompletedOrders {
  implicit def createIOSearchCompletedOrders(
    implicit client: PostgresProfile.backend.DatabaseDef,
  ) = new SearchCompletedOrders[IO] {
    def search(orderId: Int)(implicit cs: ContextShift[IO]): IO[Option[SellerCompleteTableSchemas.CompleteTableScheme]] = {
      val query = for {
        results <- SellerCompleteTable.sellerCompleteTable if results.orderId === orderId
      } yield (results)      
      IO.fromFuture(IO(client.run(query.result))).map(_.headOption)
    }
  }
}
