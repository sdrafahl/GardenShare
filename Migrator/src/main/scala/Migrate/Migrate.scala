package com.gardenShare.gardenshare.Migrator

import com.gardenShare.gardenshare.migrator.GetPostgreClient
import com.gardenShare.gardenshare.migrator._
import cats.effect.ContextShift
import cats.Applicative
import cats.implicits._
import cats.Apply
import slick.dbio.Effect
import slick.dbio.DBIOAction
import slick.dbio.NoStream
import cats.effect.IO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.JdbcBackend.DatabaseDef
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.Migrator._

abstract class MigrateDB[F[_]] {
  def up: F[Unit]
  def down: F[Unit]
}

object MigrateDB {
  def apply[F[_]]()(implicit mdb: MigrateDB[F]) = mdb
  implicit def createIOMigrator(cm: CreateMigrator[IO])(implicit db: Database, cs: ContextShift[IO]) = new MigrateDB[IO] {
    def up: IO[Unit] = IO.fromFuture(IO(db.run(cm.up)))
    def down: IO[Unit] = IO.fromFuture(IO(db.run(cm.down)))
  }     
}
