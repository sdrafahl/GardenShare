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

case class CreateMigrator[F[_]](up: DBIOAction[Unit, NoStream, Nothing], down: DBIOAction[Unit, NoStream, Nothing])
