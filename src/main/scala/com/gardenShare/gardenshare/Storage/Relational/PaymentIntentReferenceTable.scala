package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.jdbc.PostgresProfile

package object PaymentIntentReferenceTableSchemas {
  type StoreRequestId = String
  type PaymentIntentId = String
  type PaymentIntentReferenceTableSchema = (OrderId, String)
}
import PaymentIntentReferenceTableSchemas._

object PaymentIntentReferenceTable {
  class PaymentIntentReferenceTable(tag: Tag) extends Table[PaymentIntentReferenceTableSchema](tag, "paymentintentreferencetable") {
    def storeRequestId = column[OrderId]("storeorderrequestid", O.PrimaryKey)
    def paymentIntentId = column[String]("paymentintentid")
    def * = (storeRequestId, paymentIntentId)
  }
  val paymentIntentReferenceTable = TableQuery[PaymentIntentReferenceTable]
}

abstract class InsertPaymentIntentReference[F[_]] {
  def insert(storeRequestId: OrderId, paymentIntentId: String): F[Unit]
}

object InsertPaymentIntentReference {
  implicit def createIOInsertPaymentIntentReference(implicit client: PostgresProfile.backend.DatabaseDef) = new InsertPaymentIntentReference[IO] {
    def insert(storeRequestId: OrderId, paymentIntentId: String): IO[Unit] = {
      val tableQuery = PaymentIntentReferenceTable.paymentIntentReferenceTable
      val query = tableQuery.insertOrUpdate((storeRequestId, paymentIntentId))
      IO.fromFuture(IO(client.run(query.transactionally))).flatMap(_ => IO.unit)
    }
  }
}

abstract class GetPaymentIntentFromStoreRequest[F[_]] {
  def search(id: OrderId): F[Option[PaymentID]]
}

object GetPaymentIntentFromStoreRequest {
  implicit def createIOGetPaymentIntentFromStoreRequest(implicit client: PostgresProfile.backend.DatabaseDef) = new GetPaymentIntentFromStoreRequest[IO] {
    def search(id: OrderId): IO[Option[PaymentID]] = {
      val query = for {
        res <- PaymentIntentReferenceTable.paymentIntentReferenceTable if res.storeRequestId === id
      } yield res.paymentIntentId
      IO.fromFuture(IO(client.run(query.result))).map(_.headOption).map(a => a.map(b => PaymentID(b)))
    }
  }
}
