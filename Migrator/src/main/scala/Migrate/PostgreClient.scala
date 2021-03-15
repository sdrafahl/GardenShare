package com.gardenShare.gardenshare.migrator

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIOAction
import slick.lifted.AbstractTable
import cats.effect.IO
import scala.util.Try
import com.gardenShare.gardenshare.GetPostgreConfig
import javax.sql.DataSource

abstract class GetPostgreClient[F[_]] {
  def getClient: F[Database]
}

object GetPostgreClient {
  def apply[F[_]: GetPostgreClient]() = implicitly[GetPostgreClient[F]]
  implicit def IOGetPostGreLicnet(implicit g:GetPostgreConfig[IO]) = new GetPostgreClient[IO] {
    def getClient: IO[Database] = {      
      g.getConfig.flatMap(config => IO(Database.forURL(
        url = config.url,
        driver = config.driver,
        keepAliveConnection = config.keepAliveConnection
      )))
    }
  }
}
