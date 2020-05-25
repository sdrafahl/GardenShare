package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Storage.Relational.Tables.Gardens
import com.gardenShare.gardenshare.Storage.Relational.Tables.Plants


object Concurrency {
  val ec = scala.concurrent.ExecutionContext.global
  implicit val cs = IO.contextShift(ec)
}
import Concurrency._

abstract class InsertGarden[F[_]: Async] {
  def add(data: List[String]): F[List[(Int, String)]]
}

object InsertGarden {
  def apply[F[_]: InsertGarden] = implicitly[InsertGarden[F]]

  implicit object IOInsertGarden extends InsertGarden[IO] {
    def add(data: List[String]): IO[List[(Int, String)]] = {
      val query = Tables.gardens
      val qu = Tables.gardens.returning(query)
      val res = qu ++= (data.map(da => (0, da)))
      IO.fromFuture(IO(Setup.db.run(qu ++= data.map(da => (0, da))))).map(_.toList)
    }
  }
}

abstract class InsertPlant[F[_]] {
  def add(data: List[Tables.PlantData]): F[Option[Int]]
}

object InsertPlant {
  def apply[F[_]: InsertPlant] = implicitly[InsertPlant[F]]

  implicit object IOInsertPlant extends InsertPlant[IO] {
    def add(data: List[Tables.PlantData]): IO[Option[Int]] = {
      val query = Tables.plants
      val res = query ++= data
      IO.fromFuture(IO(Setup.db.run(res)))
    }
  }
}

object Tables {
  class Gardens(tag: Tag) extends Table[(Int, String)](tag, "gardens") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def owner = column[String]("owner")
    def * = (id,  owner)
  }

  val gardens = TableQuery[Gardens]
  type PlantData = (String, Int)
  class Plants(tag: Tag) extends Table[(String, Int)](tag, "plants") {
    def gardenId = column[Int]("gardenId")
    def plantName = column[String]("gardenName", O.PrimaryKey)
    def * = (plantName, gardenId)    
  }
  val plants = TableQuery[Plants]
}


object Setup {
  val db = Database.forConfig("postgres")

  val cleanupGardens: DBIO[Int] = sqlu"DROP TABLE IF EXISTS gardens;"
  val cleanupPlants: DBIO[Int] = sqlu"DROP TABLE IF EXISTS plants;"

  def createPostGresDBTables = {
    
    val setup = DBIO.seq(
      cleanupGardens,
      cleanupPlants,
      Tables.gardens.schema.create,
      Tables.plants.schema.create
    )
    IO.fromFuture(IO(db.run(setup)))
  }
}
