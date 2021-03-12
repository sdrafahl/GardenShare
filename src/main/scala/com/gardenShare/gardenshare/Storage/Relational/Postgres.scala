package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable

object PostGresSetup {
  def createPostgresClient = Database.forConfig("postgres")
}
