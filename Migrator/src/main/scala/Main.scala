package com.gardenShare.gardenshare.migrator

import slick.jdbc.PostgresProfile.api._
import cats.effect.IO
import slick.dbio.DBIOAction
import java.util.concurrent.Executors
import cats.effect._
import slick.lifted.AbstractTable
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.Migrator.RunMigrations
import com.gardenShare.gardenshare.Migrator.Migrator1

object Main extends IOApp {
  def run(args: List[String]) = {
    implicit lazy val executor = Executors.newFixedThreadPool(1)
    implicit lazy val ec = ExecutionContext.fromExecutor(executor)
    val migrator = RunMigrations[IO]()

    args.headOption match {      
      case Some("up") => {
        GetPostgreClient[IO]().getClient.flatMap {cli =>
          implicit val clientToInject = cli
          val migrations = List(
            Migrator1.createMigrator1IO
          )
          migrator.runUp(migrations)
        }.&>(IO(sys.exit(0)))
      }
      case Some("down") => {
        GetPostgreClient[IO]().getClient.flatMap {cli =>
          implicit val clientToInject = cli
          val migrations = List(
            Migrator1.createMigrator1IO
          )
          migrator.runDown(migrations)
        }.&>(IO(sys.exit(0)))
      }
      case _ => IO(println("Please specify up/down")).&>(IO(sys.exit(1))) 
    }
  }
}
