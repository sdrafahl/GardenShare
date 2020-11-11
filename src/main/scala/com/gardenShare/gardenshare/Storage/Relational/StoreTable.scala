package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import cats.effect.IO._
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.domain.Store._
import com.gardenShare.gardenshare.GoogleMapsClient._
import com.gardenShare.gardenshare.Config.GetGoogleMapsApiKey._
import com.gardenShare.gardenshare.Config.GetGoogleMapsApiKey
import cats.Monad
import cats.syntax.MonadOps._
import cats.syntax.flatMap._
import cats.implicits._
import com.gardenShare.gardenshare.GoogleMapsClient.IsWithinRange._
import com.gardenShare.gardenshare.GoogleMapsClient.IsWithinRange
import fs2.concurrent.Queue
import cats.effect.{Concurrent, ExitCode, IO, IOApp, Timer}
import fs2.Stream
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import cats.effect.concurrent.Semaphore
import cats.effect._
import scala.concurrent.duration._
import cats.effect.{Async, Fiber, CancelToken}
import cats.Parallel._
import cats.effect.concurrent.Ref
import fs2._
import fs2.interop.reactivestreams._
import cats.effect.{ContextShift, IO}
import scala.concurrent.ExecutionContext
import fs2.interop.reactivestreams._
import com.gardenShare.gardenshare.UserEntities._

object StoreTable {
  class StoreTable(tag: Tag) extends Table[(Int, String, String)](tag, "stores") {
    def storeId = column[Int]("storeId", O.PrimaryKey, O.AutoInc)
    def storeAddress = column[String]("storeAddress")
    def sellerEmail = column[String]("sellerEmail")    
    def * = (storeId, storeAddress, sellerEmail)    
  }
  val stores = TableQuery[StoreTable]
}

abstract class GetStoreByID[F[_]] {
  def getStore(id: Int): F[Option[Store]]
}

object GetStoreByID {
  implicit object GetStoreByIDIO extends GetStoreByID[IO] {
    def getStore(id: Int): IO[Option[Store]] = {
      val query = for {
        stores <- StoreTable.stores if stores.storeId equals id
      } yield (stores.storeId, stores.storeAddress, stores.sellerEmail)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(_.headOption)
        .map(_.map{l =>
          Store(l._1, Address(l._2), Email(l._3))
        })        
    }
  }
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
  def apply[F[_]: InsertStore]() = implicitly[InsertStore[F]]

  implicit object IOInsertStore extends InsertStore[IO] {
    def add(data: List[CreateStoreRequest]): IO[List[Store]] = {
      val query = StoreTable.stores
      val qu = StoreTable.stores.returning(query)
      val res = qu ++= data.map(da => (0, da.address.underlying, da.sellerEmail.underlying))
      val responses = IO.fromFuture(IO(Setup.db.run(res))).map(_.toList)
      responses.map(res => res.map(i => Store(i._1, Address(i._2), Email(i._3))))
    }
  }
  implicit class CreateStoreRequestOps(underlying: List[CreateStoreRequest]) {
    def insertStore[F[_]: InsertStore](implicit inserter: InsertStore[F]) = inserter.add(underlying)
  }
}

abstract class GetStoresStream[F[_]] {
  def getLazyStores(): Stream[F, Store]
}

object GetStoresStream {
  def apply[F[_]: GetStoresStream]() = implicitly[GetStoresStream[F]]
  implicit object Fs2GetStoresStream extends GetStoresStream[IO] {
    def getLazyStores(): Stream[IO, Store] = {
      val query = for {
        stores <- StoreTable.stores
      } yield (stores.storeId, stores.storeAddress, stores.sellerEmail)
      val reactiveStream = Setup.db.stream(query.result)      
      fromPublisher(reactiveStream).map {
        case (id, address, email) => {
          Store(id, Address(address), Email(email))
        }
      }
    }
  }
}
