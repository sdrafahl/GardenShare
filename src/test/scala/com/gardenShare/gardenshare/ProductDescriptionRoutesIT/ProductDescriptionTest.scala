package com.gardenShare.gardenshare

import utest.TestSuite
import utest.test
import utest.Tests
import com.typesafe.config.ConfigFactory
import java.net.URI
import PriceUnit.Pound
import Produce.BrownOysterMushrooms
import scala.util.Try

object ProductDescriptionTest extends TestSuite {
  lazy implicit val config = ConfigFactory.load()
  lazy implicit val dbClient = PostGresSetup.createPostgresClient
  val executionStuff = ConcurrencyHelper.createConcurrencyValues(2)
  implicit val cs = executionStuff._3

  val testEmail = "shane@gmail.com"
  val testPassword = "teST12$5jljasdf"
  val testRefreshURL = URI.create("http://localhost:3000/")
  val testReturnURL = URI.create("http://localhost:3000/")

  val tests = Tests {
    test("Product Description Routes") {
      test("productDescription/BrownOysterMushrooms") {
        println("0.000000000000000000000000000000000000000000000")
        println(Try(UserTestsHelper.makeRequestToGetProductDescription("BrownOysterMushrooms")))
        val descriptionOfMushrooms = UserTestsHelper.makeRequestToGetProductDescription("BrownOysterMushrooms")
        println("1.000000000000000000000000000000000000000000000")
        val expectedProductDescription = ProductDescription("BrownOysterMushrooms",Pound, BrownOysterMushrooms)
        println("2.000000000000000000000000000000000000000000000")
        assert(descriptionOfMushrooms equals expectedProductDescription)
      }
    }    
  }
}
