package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.domain.Store._

object StoreTable {
  class StoreTable(tag: Tag) extends Table[(Int, String, String)](tag, "stores") {
    def storeId = column[Int]("storeId", O.PrimaryKey, O.AutoInc)
    def storeAddress = column[String]("storeAddress")
    def sellerEmail = column[String]("sellerEmail")    
    def * = (storeId, storeAddress, sellerEmail)    
  }
  val stores = TableQuery[StoreTable]
}

abstract class GetStore[F[_]: Async] {
  def getStoresByUserEmail(email: Email): F[List[Store]]
}

object GetStore {
  def apply[F[_]: GetStore]() = implicitly[GetStore[F]]

  implicit object IOGetStore extends GetStore[IO]{
    def getStoresByUserEmail(email: Email): IO[List[Store]] = {
      val query = for {
        stores <- StoreTable.stores if stores.sellerEmail equals email.underlying
      } yield (stores.storeId, stores.storeAddress, stores.sellerEmail)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(lst => lst.map(
          f => Store(f._1, Address(f._2), Email(f._3))
        ))
    }
  }
}

abstract class InsertStore[F[_]] {
  def add(data: List[CreateStoreRequest]): F[List[Store]]
}

object InsertStore {
  def apply[F[_]: InsertStore] = implicitly[InsertStore[F]]

  implicit object IOInsertStore extends InsertStore[IO] {
    def add(data: List[CreateStoreRequest]): IO[List[Store]] = {
      val query = StoreTable.stores
      val qu = StoreTable.stores.returning(query)
      val res = qu ++= data.map(da => (0, da.address.underlying, da.sellerEmail.underlying))
      val responses = IO.fromFuture(IO(Setup.db.run(res))).map(_.toList)
      responses.map(res => res.map(i => Store(i._1, Address(i._2), Email(i._3))))
    }
  }
}
