package com.gardenShare.gardenshare.Config

import utest._

object ConfigSpec extends TestSuite {
  val tests = Tests{
    test("Config"){
      test("getMongoSettings") {
        test("Should return Mongo settings") {

          val testMongoHost = "www.mongo.com"
          val testMongoPort = 1234
          val testMongoPassword = "secret"
          val testMongoUsername = "testUsername"
          val expectedMongoConfig = MongoSettings(
            testMongoHost,
            testMongoPort,
            testMongoPassword,
            testMongoUsername
          )

          implicit val mockConfigDriver = new ConfigDriver {
            def getValueString(key:String) = key match {
              case "MongoHost" => testMongoHost
              case "MongoPassword" => testMongoPassword
              case "MongoUsername" => testMongoPassword
            }
            def getValueInt(key:String) = key match {
              case "MongoPort" => testMongoPort
            }
          }

          val config = Config()

          val actualMongoSettings = config.getMongoSettings

          assert(expectedMongoConfig == actualMongoSettings)
        }
      }
    }
  }
}
