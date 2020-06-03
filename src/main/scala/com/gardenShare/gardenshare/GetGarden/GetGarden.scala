package com.gardenShare.gardenshare.GetGarden

import com.gardenShare.gardenshare.Storage.Relational.GetPlant
import com.gardenShare.gardenshare.GardenData.Garden
import com.gardenShare.gardenshare.Storage.Relational.GetGardenFromDatabase
import com.gardenShare.gardenshare.Storage.Relational.GetGardenFromDatabase._
import cats.effect.IO
import com.gardenShare.gardenshare.GardenData.Plant
import cats.instances.list._
import cats.syntax.parallel._
import cats.effect.implicits._
import cats.effect.IO._
import scala.concurrent.ExecutionContext
import cats.syntax.FlattenOps._
import cats.FlatMap._

abstract class GetGarden[F[_]] {
  def getGardenByOwner(owner: String)(implicit getGarden: GetGardenFromDatabase[F], getPlant: GetPlant[F]): F[List[Garden]]
}

object GetGarden {
  def apply[F[_]: GetGarden]() = implicitly[GetGarden[F]]

  private def createPlantFromTuple(nameAndGardenId: (String, Int)) = Plant(nameAndGardenId._1)
  
  implicit object IOGetGarden extends GetGarden[IO] {
    val ec = ExecutionContext.global
    implicit val cs = IO.contextShift(ec)
    def getGardenByOwner(owner: String)(implicit getGarden: GetGardenFromDatabase[IO], getPlant: GetPlant[IO]): IO[List[Garden]] = {
      (for {
        gardenMetaData <- getGarden.getGardenByOwner(owner)
        gardens = gardenMetaData.map { ownerAndId: (String, Int) =>
          for {
            plantsData <- getPlant.gardenPlantsByGardenId(ownerAndId._2)
            plants = plantsData.map(createPlantFromTuple).toList
          } yield Garden(plants, owner)
        }.toList
      } yield gardens.parSequence).flatMap(a => a)
    }
  }
}
