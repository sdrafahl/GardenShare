package com.gardenShare.gardenshare

import utest.TestSuite
import utest.test
import utest.Tests
import com.gardenShare.gardenshare.ProductDescriptionRoutes
import org.http4s._
import org.http4s.implicits._
import cats.effect.IO
import com.gardenShare.gardenshare.Encoders.Encoders._
import com.gardenShare.gardenshare.Shows._
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import fs2.text
import io.circe.fs2._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.domain.Products.ProductDescription
import com.gardenShare.gardenshare.domain.Products.Pound
import scala.util.Try

object ProductDescriptionTest extends TestSuite {
  val tests = Tests {
    test("Product Description Routes") {
      test("productDescription/BrownOysterMushrooms") {
        val descriptionOfMushrooms = makeRequestToGetProductDescription("BrownOysterMushrooms")
        val expectedProductDescription = ProductDescription("Brown Oyster Mushrooms", Pound)
        assert(descriptionOfMushrooms equals expectedProductDescription)
      }
    }
  }

  def makeRequestToGetProductDescription(key: String) = {
    val uri = Uri.fromString(s"productDescription/${key}").toOption.get

    val request = Request[IO](Method.GET, uri)

    ProductDescriptionRoutes
      .productDescriptionRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, com.gardenShare.gardenshare.domain.Products.ProductDescription])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }
}
