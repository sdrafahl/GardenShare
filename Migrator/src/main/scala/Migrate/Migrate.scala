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
  implicit def createIOMigrator[F[_]](cm: CreateMigrator[F]) = new MigrateDB[F] {
    def up: F[Unit] = cm.up 
    def down: F[Unit] = cm.down
  }
     
}
