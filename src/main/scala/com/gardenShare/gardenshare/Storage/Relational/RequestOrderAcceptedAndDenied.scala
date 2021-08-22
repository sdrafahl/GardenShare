package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._

import cats.effect.IO
import cats.effect.ContextShift
import cats.implicits._
import slick.jdbc.PostgresProfile

object Schemas {
  type AcceptedStoreOrderRequestTableSchema = (OrderId)
  type DeniedStoreOrderRequestTableSchema = (OrderId)
}
import Schemas._

object AcceptedStoreOrderRequestTable {
  class AcceptedStoreOrderRequestTable(tag: Tag) extends Table[AcceptedStoreOrderRequestTableSchema](tag, "acceptedstoreorderrequest") {
    def storeOrderRequest = column[OrderId]("storeorderrequest")
    def * = (storeOrderRequest)
  }
  val acceptedStoreOrderRequestTable = TableQuery[AcceptedStoreOrderRequestTable]
}

object DeniedStoreOrderRequestTable {
  class DeniedStoreOrderRequestTable(tag: Tag) extends Table[DeniedStoreOrderRequestTableSchema](tag, "deniedstoreorderrequest") {
    def storeOrderRequest = column[OrderId]("storeorderrequest")
    def * = (storeOrderRequest)
  }
  val deniedStoreOrderRequestTable = TableQuery[DeniedStoreOrderRequestTable]
}

abstract class SearchAcceptedStoreOrderRequestTableByID[F[_]] {
  def search(id: OrderId)(implicit cs: ContextShift[F]): F[List[OrderId]]
}

object SearchAcceptedStoreOrderRequestTableByID {
  implicit def createIOSearchAcceptedStoreOrderRequestTableByID(implicit client: PostgresProfile.backend.DatabaseDef) = new SearchAcceptedStoreOrderRequestTableByID[IO] {
    def search(id: OrderId)(implicit cs: ContextShift[IO]): IO[List[OrderId]] = {
      val query = for {
        res <- AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable if res.storeOrderRequest === id
      } yield res.storeOrderRequest
      IO.fromFuture(IO(client.run(query.result))).map(_.toList)
    }
  }
}

abstract class SearchDeniedStoreOrderRequestTable[F[_]] {
  def search(id: OrderId)(implicit cs: ContextShift[F]): F[List[OrderId]]
}

object SearchDeniedStoreOrderRequestTable {
  implicit def createIOSearchDeniedStoreOrderRequestTable(implicit client: PostgresProfile.backend.DatabaseDef) = new SearchDeniedStoreOrderRequestTable[IO] {
    def search(id: OrderId)(implicit cs: ContextShift[IO]): IO[List[OrderId]] = {
      val query = for {
        res <- DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable if res.storeOrderRequest === id
      } yield res.storeOrderRequest
      IO.fromFuture(IO(client.run(query.result))).map(_.toList)
    }
  }
}

abstract class InsertIntoAcceptedStoreOrderRequestTableByID[F[_]] {
  def insert(a: AcceptedStoreOrderRequestTableSchema)(implicit cs: ContextShift[F]): F[AcceptedStoreOrderRequestTableSchema]
}

object InsertIntoAcceptedStoreOrderRequestTableByID {
  implicit def createIOInsertIntoAcceptedStoreOrderRequestTableByID(implicit client: PostgresProfile.backend.DatabaseDef) = new InsertIntoAcceptedStoreOrderRequestTableByID[IO] {
    def insert(a: AcceptedStoreOrderRequestTableSchema)(implicit cs: ContextShift[IO]): IO[AcceptedStoreOrderRequestTableSchema] = {
      val table = AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable
      val query = AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable.returning(table)
      val queryWithData = query +=  a
      IO.fromFuture(IO(client.run(queryWithData)))
    }
  }
}

abstract class InsertIntoDeniedStoreOrderRequestTableByID[F[_]] {
  def insert(a: AcceptedStoreOrderRequestTableSchema)(implicit cs: ContextShift[F]): F[AcceptedStoreOrderRequestTableSchema]
}

object InsertIntoDeniedStoreOrderRequestTableByID {
  implicit def createIOInsertIntoDeniedStoreOrderRequestTableByID(implicit client: PostgresProfile.backend.DatabaseDef) = new InsertIntoDeniedStoreOrderRequestTableByID[IO] {
    def insert(a: DeniedStoreOrderRequestTableSchema)(implicit cs: ContextShift[IO]): IO[DeniedStoreOrderRequestTableSchema] = {
      val table = DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable
      val query = DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable.returning(table)
      val queryWithData = query += a
      IO.fromFuture(IO(client.run(queryWithData)))
    }
  }
}
