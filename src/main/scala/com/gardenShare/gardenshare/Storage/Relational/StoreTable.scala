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
import _root_.io.circe.Encoder
import _root_.io.circe._, _root_.io.circe.generic.auto._, _root_.io.circe.parser._, _root_.io.circe.syntax._

object StoreTable {
  class StoreTable(tag: Tag) extends Table[(Int, String, String, Int, String, String)](tag, "stores") {
    def storeId = column[Int]("storeId", O.PrimaryKey, O.AutoInc)
    def street = column[String]("street")
    def city = column[String]("city")
    def zipcode = column[Int]("zip")
    def state = column[String]("state")
    def sellerEmail = column[String]("sellerEmail")    
    def * = (storeId, street, city, zipcode, state, sellerEmail)    
  }
  val stores = TableQuery[StoreTable]
}

object Helpers {
  def parseResponseForStores(response: IO[List[com.gardenShare.gardenshare.Storage.Relational.StoreTable.StoreTable#TableElementType]])(implicit d:Decoder[State]) = {
    response.map{lst =>
          lst
            .map(l => (l._1, l._2, l._3, l._4, decode[State](l._5), l._6))
            .collect{
              case (aa, bb, cc, dd, Right(ee), ff) => (aa, bb, cc, dd, ee, ff)
            }
            .map{(l: (Int, String, String, Int, State, String)) =>
              Store(l._1, Address(l._2, l._3, l._4, l._5), Email(l._6))
            }
        }
  }
}
import Helpers._

abstract class GetStoreByID[F[_]] {
  def getStore(id: Int): F[Option[Store]]
}

object GetStoreByID {
  implicit def getStoreByIDIO(implicit e: Decoder[State]) = new GetStoreByID[IO] {
    def getStore(id: Int): IO[Option[Store]] = {
      val query = for {
        stores <- StoreTable.stores if stores.storeId === id
      } yield (stores.storeId, stores.street, stores.city, stores.zipcode, stores.state, stores.sellerEmail)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(_.headOption)
        .flatMap{
          case Some(r) => decode[State](r._5) match {
            case Right(st) => IO(Some(Store(r._1, Address(r._2, r._3, r._4, st), Email(r._6))))
            case Left(err) => IO.raiseError(new Throwable(s"The state that is stored is invalid. The id of the store is ${id}"))
          }
          case None => IO.pure(None)          
        }
    }
    }
  }

abstract class GetStore[F[_]: Async] {
  def getStoresByUserEmail(email: Email): F[List[Store]]
}

object GetStore {
  def apply[F[_]: GetStore]() = implicitly[GetStore[F]]

  implicit def iOGetStore(implicit e: Decoder[State]) = new GetStore[IO]{
    def getStoresByUserEmail(email: Email): IO[List[Store]] = {
      val query = for {
        stores <- StoreTable.stores if stores.sellerEmail === email.underlying
      } yield (stores.storeId, stores.street, stores.city, stores.zipcode, stores.state, stores.sellerEmail)
      val resp = IO.fromFuture(IO(Setup.db.run(query.result))).map(_.toList)
      parseResponseForStores(resp)
    }
  }
}

abstract class InsertStore[F[_]] {
  def add(data: List[CreateStoreRequest]): F[List[Store]]
}

object InsertStore {
  def apply[F[_]: InsertStore]() = implicitly[InsertStore[F]]

  implicit def iOInsertStore(implicit e: Encoder[State], d:Decoder[State]) = new InsertStore[IO] {
    def add(data: List[CreateStoreRequest]): IO[List[Store]] = {
      val query = StoreTable.stores
      val qu = StoreTable.stores.returning(query)
      val res = qu ++= data.map(da => (0, da.address.street, da.address.city, da.address.zipcode, e(da.address.state).toString(), da.sellerEmail.underlying))
      val responses = IO.fromFuture(IO(Setup.db.run(res))).map(_.toList).map(_.toList)
      parseResponseForStores(responses)        
    }
  }

  implicit class CreateStoreRequestOps(underlying: List[CreateStoreRequest]) {
    def insertStore[F[_]: InsertStore](implicit inserter: InsertStore[F]) = inserter.add(underlying)
  }
}

abstract class DeleteStore[F[_]] {
  def delete(e: Email): F[Unit]
}

object DeleteStore {
  implicit object IODeleteStore extends DeleteStore[IO] {
    def delete(e: Email): IO[Unit] = {
      val query = (for {
        stores <- StoreTable.stores if stores.sellerEmail === e.underlying
      } yield stores).delete
      IO.fromFuture(IO(Setup.db.run(query))).map(_ => ())
    }
  }
}

abstract class GetStoresStream[F[_]] {
  def getLazyStores(): Stream[F, Store]
}

object GetStoresStream {
  def apply[F[_]: GetStoresStream]() = implicitly[GetStoresStream[F]]
  implicit def Fs2GetStoresStream(implicit d: Decoder[State]) = new GetStoresStream[IO] {
    def getLazyStores(): Stream[IO, Store] = {
      val query = for {
        stores <- StoreTable.stores
      } yield (stores.storeId, stores.street, stores.city, stores.zipcode, stores.state, stores.sellerEmail)
      val reactiveStream = Setup.db.stream(query.result)
      fromPublisher(reactiveStream).map {
        case (id, street, city, zipcode, state, email) => (id, street, city, zipcode, decode[State](state), email)
      }.collect{
        case (aa, bb, cc, dd, Right(ee), ff) => (aa, bb, cc, dd, ee, ff)
      }
        .map{(l: (Int, String, String, Int, State, String)) =>
          Store(l._1, Address(l._2, l._3, l._4, l._5), Email(l._6))
        }

    }
  }
}
