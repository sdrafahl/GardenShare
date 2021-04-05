package com.gardenShare.gardenshare

import utest.TestSuite
import utest.test
import utest.Tests
import com.gardenShare.gardenshare.ProductDescriptionRoutes
import org.http4s._
import org.http4s.implicits._
import cats.effect.IO
import com.gardenShare.gardenshare.Shows._
import fs2.text
import io.circe.fs2._
import io.circe.generic.auto._, io.circe.syntax._
import scala.util.Try
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.IA
import com.typesafe.config.ConfigFactory
import java.net.URI
import ParsingDecodingImplicits._

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
        val descriptionOfMushrooms = UserTestsHelper.makeRequestToGetProductDescription("BrownOysterMushrooms")
        val expectedProductDescription = ProductDescription("Brown-Oyster-Mushrooms",Pound,BrownOysterMushrooms)
        assert(descriptionOfMushrooms equals expectedProductDescription)
      }
    }    
  }
}
