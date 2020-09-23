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

abstract class GetNearestStores[F[_]] {
  def getNearest(n: Distance, limit: Int, fromLocation: Address)(implicit getDist: GetDistance[F], con: Concurrent[F]): F[List[Store]]
}

object GetNearestStores {
  private def isWithinRange[F[_]: GetDistance:GetGoogleMapsApiKey:Monad](range: Distance, from: Address, to: Address, getDist: GetDistance[F]): F[Boolean] = {
    for {
      dist <- getDist.getDistanceFromAddress(from, to)
    } yield dist.inRange(range)
  }

  def apply[F[_]: GetNearestStores] = implicitly[GetNearestStores[F]]
  implicit object IOGetNearestStores extends GetNearestStores[IO] {
    def getNearest(n: Distance, limit: Int, fromLocation: Address)(implicit getDist: GetDistance[IO], con: Concurrent[IO]): IO[List[Store]] = {
      val query = for {
        stores <- StoreTable.stores
      } yield (stores.storeId, stores.storeAddress, stores.sellerEmail)
      val stream = Setup.db.stream(query.result)
      for {
        queue <- Ref[IO].of(List[Store]())
        populateQueue = for {          
          populateQueProgram <- IO(stream.mapResult(a => Store(a._1, Address(a._2), Email(a._3))).mapResult {
            case Store(id, address, email) => {
              isWithinRange[IO](n, fromLocation, address, getDist).map {isInRange =>
                isInRange match {
                  case true => {
                    queue.modify(q => (List(Store(id, address, email)) ++ q, q)).attempt.unsafeRunSync()
                  }
                  case _ => IO.unit.attempt
                }
              }
            }
          }.mapResult(_.attempt.unsafeRunSync()))          
        } yield populateQueProgram

        checkOnQueueProgram = { 
            def checkQueue: IO[Unit] = {
              queue.get.map {
                case cou if cou.length < limit => {
                  Thread.sleep(1000)
                  checkQueue
                }
                case _ => ()
              }
            }
            checkQueue
          }

        _ <- IO.race(populateQueue, checkOnQueueProgram).attempt
        stores <- queue.get
      } yield stores      
    }
  }
}
