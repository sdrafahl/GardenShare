package com.gardenShare.gardenshare.migrator

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.Migrator.RunMigration
import com.gardenShare.gardenshare.Migrator.Migrator1
import scala.concurrent.duration._
import scala.language.postfixOps
import com.gardenShare.gardenshare.GetTypeSafeConfig
import com.typesafe.config.ConfigFactory
import com.gardenShare.gardenshare.GetTypeSafeConfigBoolean

object Main extends IOApp {
  def run(args: List[String]) = {
    implicit lazy val executor = Executors.newFixedThreadPool(4)
    implicit lazy val ec = ExecutionContext.fromExecutor(executor)
    val migrator = RunMigration[IO]()
    val confPgm = IO(ConfigFactory.load()).map(conf => (GetTypeSafeConfig.ioGetTypeSafeConfig(conf), GetTypeSafeConfigBoolean.createIOGetTypeSafeConfigBoolean(conf)))
    confPgm.flatMap{f =>
      implicit val gfc = f._1
      implicit val gfb = f._2
      args.headOption match {      
        case Some("up") => {
          GetPostgreClient[IO]().getClient.flatMap {cli =>
            implicit val clientToInject = cli
            val migrations = List(
              Migrator1.createMigrator1IO
            )
            migrator.runUp(migrations)
          }.map(_ => sys.exit(0))
        }
        case Some("down") => {
          GetPostgreClient[IO]().getClient.flatMap {cli =>
            implicit val clientToInject = cli
            val migrations = List(
              Migrator1.createMigrator1IO
            )
            migrator.runDown(migrations)
          }.map(_ => sys.exit(0))
        }
        case _ => IO(println("Please specify up/down")).&>(IO(sys.exit(1)))
      }
    }    
  }
}
