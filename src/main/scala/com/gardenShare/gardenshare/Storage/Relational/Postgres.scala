package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._

object Setup {
  val db = Database.forConfig("postgres") // move this into the main

  val cleanupProducts: DBIO[Int] = sqlu"DROP TABLE IF EXISTS products;"
  val cleanupStores: DBIO[Int] = sqlu"DROP TABLE IF EXISTS products;"

  def createPostGresDBTables = {

    val setup = DBIO.seq(
      cleanupProducts,
      cleanupStores,
      ProductTable.products.schema.create,
      StoreTable.stores.schema.create
    )
    IO.fromFuture(IO(db.run(setup)))
  }
}
