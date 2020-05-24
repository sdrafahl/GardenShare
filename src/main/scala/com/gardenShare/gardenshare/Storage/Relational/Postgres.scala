package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._

object PostgresDriver {
  val db = Database.forConfig("postgres")
}

object Tables {
  class Gardens(tag: Tag) extends Table[(Int, String)](tag, "GARDENS") {
    def gardenId = column[Int]("GARDEN_ID", O.PrimaryKey)
    def owner = column[String]("OWNER")
    def * = (gardenId, owner)
  }
  val gardens = TableQuery[Gardens]
  class Plants(tag: Tag) extends Table[(String, Int)](tag, "PLANTS") {
    def gardenId = column[Int]("GARDEN_ID")
    def plantName = column[String]("GARDEN_NAME", O.PrimaryKey)
    def * = (plantName, gardenId)
    def garden = foreignKey("GARDEN_ID", gardenId, gardens)(_.gardenId)
  }
  val plants = TableQuery[Plants]
}


object Setup {
  def createDBTables = {
    val ec = scala.concurrent.ExecutionContext.global
    implicit val cs = IO.contextShift(ec)
    val setup = DBIO.seq(
      (Tables.gardens.schema ++ Tables.plants.schema).create
    )
    IO.fromFuture(IO(PostgresDriver.db.run(setup)))
  }
}
