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

  implicit def createMigrator1IO(implicit cs: ContextShift[IO], db: Database): MigrateDB[IO] = {
    val dropProducts: DBIO[Int] = sqlu"DROP TABLE IF EXISTS products;"
    val dropStores: DBIO[Int] = sqlu"DROP TABLE IF EXISTS stores;"
    val dropProductReferences: DBIO[Int] = sqlu"DROP TABLE IF EXISTS productreferencetable;"
    val dropOrderRequests: DBIO[Int] = sqlu"DROP TABLE IF EXISTS storeorderrequest;"
    val dropOrderAcceptedTabelRequests: DBIO[Int] = sqlu"DROP TABLE IF EXISTS acceptedstoreorderrequest;"
    val dropOrderDeniedTabelRequests: DBIO[Int] = sqlu"DROP TABLE IF EXISTS deniedstoreorderrequest;"

    val dropCommands: DBIOAction[Unit, NoStream, Nothing] = DBIO.seq(
      dropProducts,
      dropStores,
      dropOrderRequests,
      dropProductReferences,
      dropOrderAcceptedTabelRequests,
      dropOrderDeniedTabelRequests
    )

    val createProductTable = com.gardenShare.gardenshare.Storage.Relational.ProductTable.products.schema.create
    val createStoresTable = com.gardenShare.gardenshare.Storage.Relational.StoreTable.stores.schema.create
    val createOrderRequestTable = com.gardenShare.gardenshare.Storage.Relational.StoreOrderRequestTable.storeOrderRequests.schema.create
    val productReferenceTable = com.gardenShare.gardenshare.Storage.Relational.ProductReferenceTable.productReferenceTable.schema.create
    val orderAcceptedStatusTable = com.gardenShare.gardenshare.Storage.Relational.AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable.schema.create
    val orderDeniedStatusTable = com.gardenShare.gardenshare.Storage.Relational.DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable.schema.create

    val createTables: DBIOAction[Unit, NoStream, Nothing] =  DBIO.seq(
      createProductTable,
      createStoresTable,
      createOrderRequestTable,
      productReferenceTable,
      orderAcceptedStatusTable,
      orderDeniedStatusTable
    )

    MigrateDB.createIOMigrator(CreateMigrator[IO](
      up = createTables,
      down = dropCommands
    ))
  }  
}
