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

object WorkersTable {
  class WorkerTable(tag: Tag) extends Table[(String)](tag, "workers") {
    def workerUserName = column[String]("workerUserName", O.PrimaryKey)
    def * = (workerUserName)    
  }
  lazy val workers = TableQuery[WorkerTable]
}

abstract class GetWorker[F[_]] {
  def getWorker(userName: String): F[Option[Email]]
}

object GetWorker {
  implicit object IOGetWorker extends GetWorker[IO] {
    def getWorker(userName: String): IO[Option[Email]] = {
      val query = for {
        workers <- WorkersTable.workers if workers.workerUserName equals userName
      } yield (workers)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(
          _.toList
            .headOption
            .map(x => Email(x))
        )
    }
  }
}

abstract class InsertWorker[F[_]] {
  def insertWorker(e: Email): F[Unit]
}

object InsertWorker {
  implicit object IOInsertWorker extends InsertWorker[IO] {
    def insertWorker(e: Email): IO[Unit] = {
      val query = WorkersTable.workers
      val res = query += (e.underlying)
      IO.fromFuture(IO(Setup.db.run(res))).map(_ => ())
    }
  }
}
