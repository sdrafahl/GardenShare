package com.gardenShare.gardenshare

import cats.effect.IO._
import com.gardenShare.gardenshare._
import cats.implicits._
import fs2.Stream
import cats.effect.IO
import fs2.interop.reactivestreams._
import _root_.io.circe.Encoder
import _root_.io.circe._, _root_.io.circe.parser._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile

object StoreTable {
  class StoreTable(tag: Tag) extends Table[(Int, String, String, String, String, String)](tag, "stores") {
    def storeId = column[Int]("storeId", O.PrimaryKey, O.AutoInc)
    def street = column[String]("street")
    def city = column[String]("city")
    def zipcode = column[String]("zip")
    def state = column[String]("state")
    def sellerEmail = column[String]("sellerEmail")    
    def * = (storeId, street, city, zipcode, state, sellerEmail)    
  }
  val stores = TableQuery[StoreTable]
}

object StoreTableHelpers {
  def parseResponseForStores(response: IO[List[com.gardenShare.gardenshare.StoreTable.StoreTable#TableElementType]])(implicit d:Decoder[State]) = {
    response.map{lst =>
          lst
            .map(l => (l._1, l._2, l._3, l._4, decode[State](l._5), l._6))
            .collect{
              case (aa, bb, cc, dd, Right(ee), ff) => (aa, bb, cc, dd, ee, ff)
            }
            .map{(l: (Int, String, String, String, State, String)) =>
             Email.unapply(l._6) match {
                case None => IO.raiseError(new Throwable(s"Error getting email from database msg parsed"))
                case Some(email) => IO.pure(Store(l._1, Address(l._2, l._3, l._4, l._5), email))
              }              
            }
    }
      .map(_.parSequence)
      .flatMap(x => x)
  }
}
import StoreTableHelpers._

abstract class GetStoreByID[F[_]] {
  def getStore(id: Int): F[Option[Store]]
}

object GetStoreByID {
  implicit def getStoreByIDIO(implicit e: Decoder[State], client: PostgresProfile.backend.DatabaseDef) = new GetStoreByID[IO] {
    def getStore(id: Int): IO[Option[Store]] = {
      val query = for {
        stores <- StoreTable.stores if stores.storeId === id
      } yield (stores.storeId, stores.street, stores.city, stores.zipcode, stores.state, stores.sellerEmail)
      IO.fromFuture(IO(client.run(query.result)))
        .map(_.toList)
        .map(_.headOption)
        .flatMap{
          case Some(r) => (decode[State](r._5), Email.unapply(r._6)) match {
            case (Right(st), Some(email)) => IO(Some(Store(r._1, Address(r._2, r._3, r._4, st), email)))
            case (Left(_), _) => IO.raiseError(new Throwable(s"The state that is stored is invalid. The id of the store is ${id}"))
            case (_, None) => IO.raiseError(new Throwable(s"Error parsing email: ${r._6}"))
          }
          case None => IO.pure(None)          
        }
    }
    }
  }

abstract class GetStore[F[_]] {
  def getStoresByUserEmail(email: Email): F[List[Store]]
}

object GetStore {
  def apply[F[_]: GetStore]() = implicitly[GetStore[F]]

  implicit def iOGetStore(implicit e: Decoder[State], client: PostgresProfile.backend.DatabaseDef) = new GetStore[IO]{
    def getStoresByUserEmail(email: Email): IO[List[Store]] = {
      val query = for {
        stores <- StoreTable.stores if stores.sellerEmail === email.underlying.value
      } yield (stores.storeId, stores.street, stores.city, stores.zipcode, stores.state, stores.sellerEmail)
      val resp = IO.fromFuture(IO(client.run(query.result))).map(_.toList)
      parseResponseForStores(resp)
    }
  }
}

abstract class InsertStore[F[_]] {
  def add(data: List[CreateStoreRequest]): F[List[Store]]
}

object InsertStore {
  def apply[F[_]: InsertStore]() = implicitly[InsertStore[F]]

  implicit def iOInsertStore(
    implicit e: Encoder[State],
    d:Decoder[State],
    client: PostgresProfile.backend.DatabaseDef
  ) = new InsertStore[IO] {
    def add(data: List[CreateStoreRequest]): IO[List[Store]] = {
      val query = StoreTable.stores
      val qu = StoreTable.stores.returning(query)      
      val res = qu ++= data.map(da => (0, da.address.street, da.address.city, da.address.zip, e(da.address.state).toString(), da.sellerEmail.underlying.value))
      val responses = IO.fromFuture(IO(client.run(res))).map(_.toList).map(_.toList)
      parseResponseForStores(responses)        
    }
  }

  implicit class CreateStoreRequestOps(underlying: List[CreateStoreRequest]) {
    def insertStore[F[_]: InsertStore] = implicitly[InsertStore[F]].add(underlying)
  }
}


abstract class DeleteStore[F[_]] {
  def delete(e: Email): F[Unit]
}

object DeleteStore {
  implicit def createIODeleteStore(implicit client: PostgresProfile.backend.DatabaseDef) = new DeleteStore[IO] {
    def delete(e: Email): IO[Unit] = {
      val query = (for {
        stores <- StoreTable.stores if stores.sellerEmail === e.underlying.value
      } yield stores).delete
      IO.fromFuture(IO(client.run(query))).map(_ => ())
    }
  }
}

abstract class GetStoresStream[F[_]] {
  def getLazyStores: Stream[F, Store]
}

object GetStoresStream {
  def apply[F[_]: GetStoresStream]() = implicitly[GetStoresStream[F]]
  
  implicit def Fs2GetStoresStream(
    implicit d: Decoder[State],
    client: PostgresProfile.backend.DatabaseDef    
  ) = new GetStoresStream[IO] {
    def getLazyStores: Stream[IO, Store] = {
      val query = for {
        stores <- StoreTable.stores
      } yield (stores.storeId, stores.street, stores.city, stores.zipcode, stores.state, stores.sellerEmail)
      val reactiveStream = client.stream(query.result)
      fromPublisher(reactiveStream).map {
        case (id, street, city, zipcode, state, email) => {
          (id, street, city, zipcode, decode[State](state),Email.unapply(email))
        }
      }.collect{
        case (aa, bb, cc, dd, Right(ee), Some(ff)) => (aa, bb, cc, dd, ee, ff)
      }
        .map{(l: (Int, String, String, String, State, Email)) =>
          Store(l._1, Address(l._2, l._3, l._4, l._5), l._6)          
        }
    }
  }
}
