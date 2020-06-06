package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._

object Plants {
type PlantData = (String, Int)
  class Plants(tag: Tag) extends Table[(String, Int)](tag, "plants") {
    def gardenId = column[Int]("gardenId")
    def plantName = column[String]("gardenName", O.PrimaryKey)
    def * = (plantName, gardenId)    
  }
  val plants = TableQuery[Plants]
}

abstract class GetPlant[F[_]: Async] {
  def gardenPlantsByGardenId(id: Int): F[Seq[(String, Int)]]
}

object GetPlant {
  def apply[F[_]: GetPlant]() = implicitly[GetPlant[F]]

  implicit object IOGetPlant {
    def gardenPlantsByGardenId(id: Int): IO[Seq[(String, Int)]] = {
      val query = for {
        plants <- Plants.plants if plants.gardenId equals id
      } yield (plants.plantName, plants.gardenId)
      IO.fromFuture(IO(Setup.db.run(query.result)))
    }
  }
}

abstract class InsertPlant[F[_]] {
  def add(data: List[Plants.PlantData]): F[Option[Int]]
}

object InsertPlant {
  def apply[F[_]: InsertPlant] = implicitly[InsertPlant[F]]

  implicit object IOInsertPlant extends InsertPlant[IO] {
    def add(data: List[Plants.PlantData]): IO[Option[Int]] = {
      val query = Plants.plants
      val res = query ++= data
      IO.fromFuture(IO(Setup.db.run(res)))
    }
  }
}
