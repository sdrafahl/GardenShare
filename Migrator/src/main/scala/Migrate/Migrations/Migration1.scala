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
import com.gardenShare.gardenshare.Storage.Relational.ProductTable._

object Migrator1 {

  implicit def createMigrator1IO(implicit db: Database, cs: ContextShift[IO]): MigrateDB[IO] = {
    val dropProducts: DBIO[Int] = sqlu"DROP TABLE IF EXISTS products;"
    val dropStores: DBIO[Int] = sqlu"DROP TABLE IF EXISTS stores;"
    val dropOrders: DBIO[Int] = sqlu"DROP TABLE IF EXISTS orders;"
    val dropCommands = DBIO.seq(
      dropProducts,
      dropStores,
      dropOrders
    )

    val createProductTable = com.gardenShare.gardenshare.Storage.Relational.ProductTable.products.schema.create
    val createStoresTable = com.gardenShare.gardenshare.Storage.Relational.StoreTable.stores.schema.create
    val createOrdersTable = com.gardenShare.gardenshare.Storage.Relational.OrderTable.orders.schema.create

    val createTables =  DBIO.seq(
      createProductTable,
      createStoresTable,
      createOrdersTable
    )

    MigrateDB.createIOMigrator(CreateMigrator[IO](
      up = IO.fromFuture(IO(db.run(createTables))),
      down = IO.fromFuture(IO(db.run(dropCommands)))
    ))
  }  
}
