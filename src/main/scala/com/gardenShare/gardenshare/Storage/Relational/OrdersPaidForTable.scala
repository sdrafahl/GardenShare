package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.jdbc.PostgresProfile

object OrdersPaidForTableSchemas {
  type OrdersPaidForTableSchema = (OrderId)
}

import OrdersPaidForTableSchemas._

object OrdersPaidForTable {
  class OrdersPaidForTable(tag: Tag) extends Table[OrdersPaidForTableSchema](tag, "orderspaidfortable") {
    def orderId = column[OrderId]("orderid", O.PrimaryKey)
    def * = (orderId)
  }
  val ordersPaidForTable = TableQuery[OrdersPaidForTable]
}

abstract class OrderIdIsPaidFor[F[_]] {
  def isPaidFor(order: OrderId): F[Boolean]
}

object OrderIdIsPaidFor {
  implicit def createIOOrderIdIsPaidFor(implicit client: PostgresProfile.backend.DatabaseDef) = new OrderIdIsPaidFor[IO] {
    def isPaidFor(order: OrderId): IO[Boolean] = {
      val query = for {
        res <- OrdersPaidForTable.ordersPaidForTable if res.orderId === order
      } yield res
      IO.fromFuture(IO(client.run(query.result))).map(_.headOption).map{
        case None => false
        case Some(_) => true
      }
    }
  }
}

abstract class SetOrderIsPaid[F[_]] {
  def setOrder(orderId: OrderId): F[Unit]
}

object SetOrderIsPaid {
  implicit def createIOSetOrderIsPaid(implicit client: PostgresProfile.backend.DatabaseDef) = new SetOrderIsPaid[IO] {
    def setOrder(orderId: OrderId): IO[Unit] = {
      val table = OrdersPaidForTable.ordersPaidForTable
      val baseQuery = OrdersPaidForTable.ordersPaidForTable.returning(table)
      val query = (baseQuery += (orderId)).transactionally
      IO.fromFuture(IO(client.run(query))).map(_ => ())
    }
  }
}
