package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.lifted.AbstractTable
import slick.jdbc.PostgresProfile
import cats.effect.ContextShift
import scala.concurrent.ExecutionContext

object StripeAccountEmailTableSchemas {
  type StripeAccountEmailTableSchema = (String, String) 
}
import StripeAccountEmailTableSchemas._

object StripeAccountEmailTable {
  class StripeAccountEmailTable(tag: Tag) extends Table[StripeAccountEmailTableSchema](tag, "stripeaccountemailtable") {
    def userEmail = column[String]("userEmail")
    def stripeAccountId = column[String]("stripeAccountId")
    def * = (userEmail, stripeAccountId)
  }
  val stripeAccountEmailTable = TableQuery[StripeAccountEmailTable]
}

abstract class SearchAccountIdsByEmail[F[_]] {
  def search(e: Email)(implicit cs: ContextShift[F]): F[Option[String]]
}

object SearchAccountIdsByEmail {
  implicit def createIOSearchAccountIdsByEmail(implicit client: PostgresProfile.backend.DatabaseDef) = new SearchAccountIdsByEmail[IO] {
    def search(e: Email)(implicit cs: ContextShift[IO]): IO[Option[String]] = {
      val query = for {
        res <- StripeAccountEmailTable.stripeAccountEmailTable if res.userEmail === e.underlying.value
      } yield res.stripeAccountId
      IO.fromFuture(IO(client.run(query.result))).map(_.headOption)
    }
  }
}

abstract class InsertAccountEmailReference[F[_]] {
  def insert(slickAccountId: String, accountEmail: Email)(implicit cs: ContextShift[F], ec: ExecutionContext): F[Unit]
}

object InsertAccountEmailReference {
  implicit def createIOInsertAccountEmailReference(implicit client: PostgresProfile.backend.DatabaseDef) = new InsertAccountEmailReference[IO] {
    def insert(slickAccountId: String, accountEmail: Email)(implicit cs: ContextShift[IO], ec: ExecutionContext): IO[Unit] = {
      val tableBaseQuery = StripeAccountEmailTable.stripeAccountEmailTable
      val request = StripeAccountEmailTable.stripeAccountEmailTable.returning(tableBaseQuery)
      val requestWithData = request += (accountEmail.underlying.value,slickAccountId)
      val deleteQuery = StripeAccountEmailTable.stripeAccountEmailTable.filter(f => f.userEmail === accountEmail.underlying.value).delete

      val query = (for {
        _ <- deleteQuery
        insertQuery <- requestWithData
      } yield insertQuery).transactionally

      IO.fromFuture(IO(client.run(query))).map(_ => ())      
    }
  }
}

abstract class DeleteAccountEmailReferences[F[_]] {
  def delete(e: Email)(implicit cs: ContextShift[F]): F[Unit]
}

object DeleteAccountEmailReferences {
  implicit def createIODeleteAccountEmailReferences(implicit client: PostgresProfile.backend.DatabaseDef) = new DeleteAccountEmailReferences[IO] {
    def delete(e: Email)(implicit cs: ContextShift[IO]): IO[Unit] = {
      val query = for {
        res <- StripeAccountEmailTable.stripeAccountEmailTable if res.userEmail === e.underlying.value
      } yield res
      IO.fromFuture(IO(client.run(query.delete))).map(_ => ())
    }
  }
}
