package com.gardenShare.gardenshare.Config

import utest._
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.InsertGarden
import com.gardenShare.gardenshare.Storage.Relational.Gardens._
import com.gardenShare.gardenshare.Storage.Relational.Plants._
import com.gardenShare.gardenshare.Storage.Relational.InsertPlant
import com.typesafe.config.Config

object PersistentStorageSpec extends TestSuite {
  val tests = Tests{
    test("GetUserPoolName"){
      test("for IO") {
        test("exec") {
          test("Should provide the name of the user pool given a fake typeSafeConfig") {
            val testUserPoolName = "standardUserPoolName"
            val expectedUserPoolName = UserPoolName(testUserPoolName)
            implicit val fakeGetTypeSafeConfig = new GetTypeSafeConfig[IO] {
              def get(key: String) = key match {
                case "users.standardUserPoolName" => IO(testUserPoolName)
              }
            }

            val testGetUserPoolName = GetUserPoolName[IO]

            val returnedUserPool = testGetUserPoolName.exec().unsafeRunSync()

            assert(returnedUserPool == expectedUserPoolName)
          }
        }
      }
    }
  }
}
