package com.gardenShare.gardenshare.migrator

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIOAction
import slick.lifted.AbstractTable
import cats.effect.IO

abstract class GetPostgreClient[F[_]] {
  def getClient: F[Database]
}

object GetPostgreClient {
  def apply[F[_]: GetPostgreClient]() = implicitly[GetPostgreClient[F]]
  implicit object IOGetPostGreLicnet extends GetPostgreClient[IO] {
    def getClient: IO[Database] = IO(Database.forConfig("postgres"))
  }
}
