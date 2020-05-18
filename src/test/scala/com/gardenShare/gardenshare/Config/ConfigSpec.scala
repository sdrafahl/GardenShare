package com.gardenShare.gardenshare.Config

import utest._
import org.mockito.Mockito
import org.mockito.Matchers
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import org.mockito.Mock

object ConfigSpec extends TestSuite with MockitoSugar with ArgumentMatchersSugar {
  val tests = Tests{
    //implicit val mockConfigDrivers = mock[ConfigDriver]("mockConfigDrivers")
    test("Config"){
      test("getMongoSettings") {
        test("Should return Mongo settings") {
          val testMongoHost = "www.mongo.com"
          val testMongoHostKey = "mongoHostKey"
          val expectedMongoConfig = MongoSettings(testMongoHost)

          implicit val mockConfigDriver = new ConfigDriver {
            def getValueString(key:String) = key match {
              case testMongoHostKey => testMongoHost
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
