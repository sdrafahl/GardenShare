package com.gardenShare.gardenshare.Config

import utest._
import cats.effect.IO
import com.gardenShare.gardenshare.CreateGarden.CreateGarden
import com.gardenShare.gardenshare.Storage.Relational.InsertGarden
import com.gardenShare.gardenshare.Storage.Relational.Gardens._
import com.gardenShare.gardenshare.Storage.Relational.Plants._
import com.gardenShare.gardenshare.Storage.Relational.InsertPlant
import com.gardenShare.gardenshare.GardenData.Garden
import com.gardenShare.gardenshare.GardenData.Plant
import com.gardenShare.gardenshare.CreateGarden.CreateGarden._

object PersistentStorageSpec extends TestSuite {
  val tests = Tests{
    test("CreateGarden"){
      test("for IO") {
        test("createGarden") {
          test("Should use the provided Insert typeclass for the database") {

            val testGardenId = 0
            val testGardenOwner = "me"
            val testPlantName = "mint"
            val gardenId = 0

            implicit val mockInsertGarden = new InsertGarden[IO] {
              def add(data: List[String]): IO[List[(Int, String)]] = {
                assert(data.contains(testGardenOwner))
                IO(
                  List((gardenId, testGardenOwner))
                )
              }
            }

            implicit val mockInsertPlant = new InsertPlant[IO] {
              def add(data: List[(String, Int)]): IO[Option[Int]] = {
                assert(data.contains((testPlantName, testGardenId)))
                IO(
                  Some(0)
                )
              }
            }
            
            val garden = Garden(List(Plant(testPlantName)), testGardenOwner)
            val createGarden = CreateGarden[IO]
            garden.create.unsafeRunSync()
          }
        }
      }
    }
  }
}
