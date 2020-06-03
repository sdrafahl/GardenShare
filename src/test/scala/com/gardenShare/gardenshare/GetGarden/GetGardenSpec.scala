package com.gardenShare.gardenshare.GetGarden

import utest._
import cats.effect.IO
import com.gardenShare.gardenshare.CreateGarden.CreateGarden
import com.gardenShare.gardenshare.Storage.Relational.InsertGarden
import com.gardenShare.gardenshare.Storage.Relational.Tables
import com.gardenShare.gardenshare.Storage.Relational.InsertPlant
import com.gardenShare.gardenshare.GardenData.Garden
import com.gardenShare.gardenshare.GardenData.Plant
import com.gardenShare.gardenshare.CreateGarden.CreateGarden._
import org.mockito.internal.handler.MockHandlerFactory
import com.gardenShare.gardenshare.Storage.Relational.GetGardenFromDatabase
import com.gardenShare.gardenshare.Storage.Relational.GetPlant

object GetGardenSpec extends TestSuite {
  val tests = Tests {
    test("GetGarden") {
      test("for IO") {
        test("getGardenByOwner") {
          test("Should use the provided get garden and plant typeclasses to get a garden") {
            val testOwner = "testOwner"
            val testPlantName = "plantName"
            val testGardenId = 0
            val expectedGarden = Garden(List(Plant(testPlantName)), testOwner)
            implicit val mockGetGardenFromDB = new GetGardenFromDatabase[IO] {
              def getGardenByOwner(owner: String): IO[Seq[(String, Int)]] = {
                assert(owner equals testOwner)
                IO(Seq((owner, testGardenId)))
              }
            }
            implicit val mockGetPlant = new GetPlant[IO] {
              def gardenPlantsByGardenId(id: Int): IO[Seq[(String, Int)]] = {
                assert(id equals testGardenId)
                IO(Seq((testPlantName, testGardenId)))
              }
            }
            val gardens = GetGarden[IO].getGardenByOwner(testOwner).unsafeRunSync()
            assert(gardens equals List(expectedGarden))
          }
        }
      }
    }
  }
}
