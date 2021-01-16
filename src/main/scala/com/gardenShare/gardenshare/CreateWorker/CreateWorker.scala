package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities._
import com.gardenShare.gardenshare.Storage.Relational
import com.gardenShare.gardenshare.Storage.Relational.InsertWorker
import cats.effect.IO
import cats.implicits._

abstract class CreateWorker[F[_]] {
  def createWorker(user: Email): F[CreateWorkerResponse]
}

object CreateWorker {
  def apply[F[_]: CreateWorker]() = implicitly[CreateWorker[F]]
  implicit def createIOCreateWorker(implicit insertWorker: InsertWorker[IO]) = new CreateWorker[IO] {
    def createWorker(user: Email): IO[CreateWorkerResponse] = {
      insertWorker
        .insertWorker(user)
        .attempt
        .map{
          case Left(err) => WorkerFailedToCreate(s"Worker failed to create ${err.getMessage()}")
          case Right(res) => WorkerCreatedSuccessfully()
        }
    }
  }

  implicit class CreateWorkerOps(underlying: Email) {
    def submitWorker[F[_]: CreateWorker] = implicitly[CreateWorker[F]].createWorker(underlying)
  }
}
