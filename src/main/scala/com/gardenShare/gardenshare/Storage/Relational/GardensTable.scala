package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._p

object Gardens {
  class Gardens(tag: Tag) extends Table[(Int, String)](tag, "gardens") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def owner = column[String]("owner")
    def * = (id,  owner)
  }

  val gardens = TableQuery[Gardens]
}

abstract class GetGardenFromDatabase[F[_]: Async] {
  def getGardenByOwner(owner: String): F[Seq[(String, Int)]]
}

object GetGardenFromDatabase {
  def apply[F[_]: GetGardenFromDatabase] = implicitly[GetGardenFromDatabase[F]]

  implicit object IOGetGardenByOwner extends GetGardenFromDatabase[IO] {
    def getGardenByOwner(owner: String): IO[Seq[(String, Int)]] = {
      val query = for {
        garden <- Gardens.gardens if garden.owner equals owner
      } yield (garden.owner, garden.id)
      IO.fromFuture(IO(Setup.db.run(query.result)))
    }
  }
}

abstract class InsertGarden[F[_]: Async] {
  def add(data: List[String]): F[List[(Int, String)]]
}

object InsertGarden {
  def apply[F[_]: InsertGarden] = implicitly[InsertGarden[F]]

  implicit object IOInsertGarden extends InsertGarden[IO] {
    def add(data: List[String]): IO[List[(Int, String)]] = {
      val query = Gardens.gardens
      val qu = Gardens.gardens.returning(query)
      val res = qu ++= (data.map(da => (0, da)))
      IO.fromFuture(IO(Setup.db.run(qu ++= data.map(da => (0, da))))).map(_.toList)
    }
  }
}
