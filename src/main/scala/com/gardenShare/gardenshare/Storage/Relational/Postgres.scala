package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._

object Setup {
  val db = Database.forConfig("postgres")

  val cleanupGardens: DBIO[Int] = sqlu"DROP TABLE IF EXISTS gardens;"
  val cleanupPlants: DBIO[Int] = sqlu"DROP TABLE IF EXISTS plants;"

  def createPostGresDBTables = {
    
    val setup = DBIO.seq(
      cleanupGardens,
      cleanupPlants,
      Gardens.gardens.schema.create,
      Plants.plants.schema.create
    )
    IO.fromFuture(IO(db.run(setup)))
  }
}
