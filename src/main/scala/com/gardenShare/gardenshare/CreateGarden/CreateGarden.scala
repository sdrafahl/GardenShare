package com.gardenShare.gardenshare.CreateGarden

import com.gardenShare.gardenshare.GardenData.Garden
import com.gardenShare.gardenshare.Storage.Relational.InsertGarden
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.InsertPlant
import com.gardenShare.gardenshare.GardenData.Plant
import cats.implicits._

abstract class CreateGarden[F[_]: InsertGarden: InsertPlant] {
  def createGarden(garden: Garden)(implicit insertGarden: InsertGarden[F], insertPlant: InsertPlant[F]): F[Unit]
}

object CreateGarden {
  def apply[F[_]: CreateGarden] = implicitly[CreateGarden[F]]

  implicit class CreateGardenOps[F[_]: CreateGarden:InsertGarden:InsertPlant](garden: Garden) {
    def create(implicit createGarden: CreateGarden[F]) = {
      createGarden.createGarden(garden) 
    }
  }

  implicit object IOCreateGarden extends CreateGarden[IO] {
    def createGarden(garden: Garden)(implicit insertGarden: InsertGarden[IO], insertPlant: InsertPlant[IO]): IO[Unit] = {      
      for {
        results <- insertGarden.add(List(garden.owner))
        plants = results.flatMap { gardenOwner =>
          garden.plants.map { plant: Plant =>
            insertPlant.add(
              List(
                (plant.name, 0)
              )
            )
          }
        }
      } yield plants.sequence
    }
  }
}
