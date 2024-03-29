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
import com.gardenShare.gardenshare.ProductTable._

object Migrator1 {

  implicit def createMigrator1IO(implicit cs: ContextShift[IO], db: Database): MigrateDB[IO] = {
    val dropProducts: DBIO[Int] = sqlu"DROP TABLE IF EXISTS products;"
    val dropStores: DBIO[Int] = sqlu"DROP TABLE IF EXISTS stores;"
    val dropProductReferences: DBIO[Int] = sqlu"DROP TABLE IF EXISTS productreferencetable;"
    val dropOrderRequests: DBIO[Int] = sqlu"DROP TABLE IF EXISTS storeorderrequest;"
    val dropOrderAcceptedTabelRequests: DBIO[Int] = sqlu"DROP TABLE IF EXISTS acceptedstoreorderrequest;"
    val dropOrderDeniedTabelRequests: DBIO[Int] = sqlu"DROP TABLE IF EXISTS deniedstoreorderrequest;"
    val dropSlickEmailRefRequest: DBIO[Int] = sqlu"DROP TABLE IF EXISTS stripeaccountemailtable;"
    val dropOrdersPaidForTable = sqlu"DROP TABLE IF EXISTS orderspaidfortable"
    val dropPaymentIntentReferenceTable = sqlu"DROP TABLE IF EXISTS paymentintentreferencetable"
    val dropSellerCompleteTable = sqlu"DROP TABLE IF EXISTS sellercompletetable"
    val dropBuyerOrderCompleteTable = sqlu"DROP TABLE IF EXISTS buyerordercompletetable"

    val dropCommands: DBIOAction[Unit, NoStream, Nothing] = DBIO.seq(
      dropProducts,
      dropStores,
      dropOrderRequests,
      dropProductReferences,
      dropOrderAcceptedTabelRequests,
      dropOrderDeniedTabelRequests,
      dropSlickEmailRefRequest,
      dropOrdersPaidForTable,
      dropPaymentIntentReferenceTable,
      dropSellerCompleteTable,
      dropBuyerOrderCompleteTable
    )

    val createProductTable = com.gardenShare.gardenshare.ProductTable.products.schema.create
    val createStoresTable = com.gardenShare.gardenshare.StoreTable.stores.schema.create
    val createOrderRequestTable = com.gardenShare.gardenshare.StoreOrderRequestTable.storeOrderRequests.schema.create
    val productReferenceTable = com.gardenShare.gardenshare.ProductReferenceTable.productReferenceTable.schema.create
    val orderAcceptedStatusTable = com.gardenShare.gardenshare.AcceptedStoreOrderRequestTable.acceptedStoreOrderRequestTable.schema.create
    val orderDeniedStatusTable = com.gardenShare.gardenshare.DeniedStoreOrderRequestTable.deniedStoreOrderRequestTable.schema.create
    val slickAccountEmailRef = com.gardenShare.gardenshare.StripeAccountEmailTable.stripeAccountEmailTable.schema.create
    val ordersPaidForTable = com.gardenShare.gardenshare.OrdersPaidForTable.ordersPaidForTable.schema.create
    val paymentIntentReferenceTableScheme = com.gardenShare.gardenshare.PaymentIntentReferenceTable.paymentIntentReferenceTable.schema.create
    val createSellerCompleteTable = com.gardenShare.gardenshare.SellerCompleteTable.sellerCompleteTable.schema.create
    val createBuyerOrderCompleteTable = com.gardenShare.gardenshare.BuyerOrderCompleteTable.buyerOrderCompleteTable.schema.create
   
    val createTables: DBIOAction[Unit, NoStream, Nothing] =  DBIO.seq(
      createProductTable,
      createStoresTable,
      createOrderRequestTable,
      productReferenceTable,
      orderAcceptedStatusTable,
      orderDeniedStatusTable,
      slickAccountEmailRef,
      ordersPaidForTable,
      paymentIntentReferenceTableScheme,
      createSellerCompleteTable,
      createBuyerOrderCompleteTable
    )

    MigrateDB.createIOMigrator(CreateMigrator[IO](
      up = createTables,
      down = dropCommands
    ))
  }  
}
