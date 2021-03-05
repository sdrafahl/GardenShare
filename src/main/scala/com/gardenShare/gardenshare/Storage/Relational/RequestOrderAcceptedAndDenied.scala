package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIOAction
import slick.lifted.AbstractTable

import cats.effect.IO
import com.gardenShare.gardenshare.StoreOrderRequest
import cats.effect.ContextShift
import cats.implicits._
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.StoreOrderRequestWithId
import java.time.ZonedDateTime
import scala.util.Try
import com.gardenShare.gardenshare.ParseZoneDateTime
import com.gardenShare.gardenshare.ProductAndQuantity
import com.gardenShare.gardenshare.ParseDate

object Schemas {
  type AcceptedStoreOrderRequestTableSchema = (Int)
  type DeniedStoreOrderRequestTableSchema = (Int)
}
import Schemas._

object AcceptedStoreOrderRequestTable {
  class AcceptedStoreOrderRequestTable(tag: Tag) extends Table[AcceptedStoreOrderRequestTableSchema](tag, "acceptedstoreorderrequest") {
    def storeOrderRequest = column[Int]("storeorderrequest")
    def * = (storeOrderRequest)
  }
  val acceptedStoreOrderRequestTable = TableQuery[AcceptedStoreOrderRequestTable]
}

object DeniedStoreOrderRequestTable {
  class DeniedStoreOrderRequestTable(tag: Tag) extends Table[DeniedStoreOrderRequestTableSchema](tag, "acceptedstoreorderrequest") {
    def storeOrderRequest = column[Int]("storeorderrequest")
    def * = (storeOrderRequest)
  }
  val deniedStoreOrderRequestTable = TableQuery[DeniedStoreOrderRequestTable]
}

abstract class SearchAcceptedStoreOrderRequestTableByID[F[_]] {
  def search(id: Int)(implicit cs: ContextShift[F]): F[List[Int]]
}

object SearchAcceptedStoreOrderRequestTableByID {
  implicit object IOSearchAcceptedStoreOrderRequestTableByID extends SearchAcceptedStoreOrderRequestTableByID[IO] {
    def search(id: Int)(implicit cs: ContextShift[IO]): IO[List[Int]] = {
      val query = for {
        res <- AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable if res.storeOrderRequest === id
      } yield res.storeOrderRequest
      IO.fromFuture(IO(Setup.db.run(query.result))).map(_.toList)
    }
  }
}

abstract class SearchDeniedStoreOrderRequestTable[F[_]] {
  def search(id: Int)(implicit cs: ContextShift[F]): F[List[Int]]
}

object SearchDeniedStoreOrderRequestTable {
  implicit object IOSearchDeniedStoreOrderRequestTable extends SearchDeniedStoreOrderRequestTable[IO] {
    def search(id: Int)(implicit cs: ContextShift[IO]): IO[List[Int]] = {
      val query = for {
        res <- DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable if res.storeOrderRequest === id
      } yield res.storeOrderRequest
      IO.fromFuture(IO(Setup.db.run(query.result))).map(_.toList)
    }
  }
}

abstract class InsertIntoAcceptedStoreOrderRequestTableByID[F[_]] {
  def insert(a: AcceptedStoreOrderRequestTableSchema)(implicit cs: ContextShift[F]): F[AcceptedStoreOrderRequestTableSchema]
}

object InsertIntoAcceptedStoreOrderRequestTableByID {
  implicit object IOInsertIntoAcceptedStoreOrderRequestTableByID extends InsertIntoAcceptedStoreOrderRequestTableByID[IO] {
    def insert(a: AcceptedStoreOrderRequestTableSchema)(implicit cs: ContextShift[IO]): IO[AcceptedStoreOrderRequestTableSchema] = {
      val table = AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable
      val query = AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable.returning(table)
      val queryWithData = query +=  a
      IO.fromFuture(IO(Setup.db.run(queryWithData)))
    }
  }
}

abstract class InsertIntoDeniedStoreOrderRequestTableByID[F[_]] {
  def insert(a: AcceptedStoreOrderRequestTableSchema)(implicit cs: ContextShift[F]): F[AcceptedStoreOrderRequestTableSchema]
}

object InsertIntoDeniedStoreOrderRequestTableByID {
  implicit object IOInsertIntoDeniedStoreOrderRequestTableByID extends InsertIntoDeniedStoreOrderRequestTableByID[IO] {
    def insert(a: DeniedStoreOrderRequestTableSchema)(implicit cs: ContextShift[IO]): IO[DeniedStoreOrderRequestTableSchema] = {
      val table = DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable
      val query = DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable.returning(table)
      val queryWithData = query += a
      IO.fromFuture(IO(Setup.db.run(queryWithData)))
    }
  }
}
