package com.gardenShare.gardenshare

import utest.TestSuite
import utest.test
import utest.Tests
import com.gardenShare.gardenshare.ProductDescriptionRoutes
import org.http4s._
import org.http4s.implicits._
import cats.effect.IO
import com.gardenShare.gardenshare.Encoders._
import com.gardenShare.gardenshare.Shows._
import fs2.text
import io.circe.fs2._
import io.circe.generic.auto._, io.circe.syntax._
import scala.util.Try
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.IA
import com.typesafe.config.ConfigFactory

object ProductDescriptionTest extends TestSuite {
  lazy implicit val config = ConfigFactory.load()
  lazy implicit val dbClient = PostGresSetup.createPostgresClient
  val executionStuff = ConcurrencyHelper.createConcurrencyValues(2)
  implicit val cs = executionStuff._3

  val testEmail = "shane@gmail.com"
  val testPassword = "teST12$5jljasdf"

  val tests = Tests {
    test("Product Description Routes") {
      test("productDescription/BrownOysterMushrooms") {
        val descriptionOfMushrooms = UserTestsHelper.makeRequestToGetProductDescription("BrownOysterMushrooms")
        val expectedProductDescription = ProductDescription("Brown-Oyster-Mushrooms",Pound,BrownOysterMushrooms)
        assert(descriptionOfMushrooms equals expectedProductDescription)
      }
    }
    test("A user should be created, apply to be a seller, and then add a product to the sellers store") {
      UserTestsHelper.deleteUserAdmin(testEmail)
      UserTestsHelper.deletestore(Email(testEmail))
      UserTestsHelper.adminCreateUser(testEmail, testPassword)
      val r = UserTestsHelper.authUser(testEmail, testPassword)
      val jwtToken = r.auth.get.jwt
      val address = Address("500 hickman Rd", "Waukee", "50263", IA)
      UserTestsHelper.applyUserToBecomeSeller(jwtToken, address)     
      val response = UserTestsHelper.addProductToStore("BrownOysterMushrooms", jwtToken, Amount(100, USD))
      val productsInStore = UserTestsHelper.getProductsFromStore(jwtToken).listOfProduce.map(f => f.product.productName)
      assert(productsInStore equals List(BrownOysterMushrooms))
    }
  }
}
