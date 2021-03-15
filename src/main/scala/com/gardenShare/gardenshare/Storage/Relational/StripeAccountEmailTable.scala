package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.lifted.AbstractTable
import slick.jdbc.PostgresProfile
import cats.effect.ContextShift

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
        res <- StripeAccountEmailTable.stripeAccountEmailTable if res.userEmail === e.underlying
      } yield res.stripeAccountId
      IO.fromFuture(IO(client.run(query.result))).map(_.headOption)
    }
  }
}

abstract class InsertAccountEmailReference[F[_]] {
  def insert(slickAccountId: String, accountEmail: Email)(implicit cs: ContextShift[F]): F[Unit]
}

object InsertAccountEmailReference {
  implicit def createIOInsertAccountEmailReference(implicit client: PostgresProfile.backend.DatabaseDef) = new InsertAccountEmailReference[IO] {
    def insert(slickAccountId: String, accountEmail: Email)(implicit cs: ContextShift[IO]): IO[Unit] = {
      val tableBaseQuery = StripeAccountEmailTable.stripeAccountEmailTable
      val request = StripeAccountEmailTable.stripeAccountEmailTable.returning(tableBaseQuery)
      val requestWithData = request += (accountEmail.underlying,slickAccountId)

      val deleteQuery = for {
        res <- StripeAccountEmailTable.stripeAccountEmailTable if res.userEmail === accountEmail.underlying
      } yield res

      val deletePgm:IO[Int] = IO.fromFuture(IO(client.run(deleteQuery.delete)))
      deletePgm.flatMap{_ => IO.fromFuture(IO(client.run(requestWithData))).map(_ => ())}      
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
        res <- StripeAccountEmailTable.stripeAccountEmailTable if res.userEmail === e.underlying
      } yield res
      IO.fromFuture(IO(client.run(query.delete))).map(_ => ())
    }
  }
}
