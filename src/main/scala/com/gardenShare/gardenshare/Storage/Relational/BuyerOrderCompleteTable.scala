package com.gardenShare.gardenshare

import slick.jdbc.PostgresProfile.api._

object BuyerOrderCompleteTableSchemas {
  type BuyerOrderCompleteTableScheme = (OrderId)
}
import BuyerOrderCompleteTableSchemas._

object BuyerOrderCompleteTable {
  class BuyerOrderCompleteTable(tag: Tag) extends Table[BuyerOrderCompleteTableScheme](tag, "buyerordercompletetable") {
    def order = column[OrderId]("order")
    def * = (order)
  }
  val buyerOrderCompleteTable = TableQuery[BuyerOrderCompleteTable]
}
