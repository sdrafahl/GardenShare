package com.gardenShare.gardenshare

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import fs2.text
import io.circe.fs2._
import io.circe.generic.auto._, io.circe.syntax._
import utest.TestSuite
import utest.test
import utest.Tests
import com.gardenShare.gardenshare.domain.User.UserInfo
import com.gardenShare.gardenshare.UserEntities.Sellers
import com.gardenShare.gardenshare.Encoders.Encoders._
import com.gardenShare.gardenshare.Shows._
import com.gardenShare.gardenshare.Storage.Relational.DeleteStore
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import com.gardenShare.gardenshare.domain.Store.Address
import com.gardenShare.gardenshare.domain.Store.IA
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import com.gardenShare.gardenshare.domain.Store._

object StoreOrderRequestsTest extends TestSuite {
  val tests = Tests {
    test("Store Order Request") {
      val testBuyerEmail = "testbuyer@gmail.com"
      val testpassword = "testPass12123$"
      val testSellerEmail = "testseller@gmail.com"
      test("/storeOrderRequest/testseller@gmail.com") {
        UserTestsHelper.deleteUserAdmin(testBuyerEmail)
        UserTestsHelper.deleteUserAdmin(testSellerEmail)
        UserTestsHelper.deleteAllStoreOrdersForSeller[IO](testSellerEmail).unsafeRunSync()

        UserTestsHelper.adminCreateUser(testBuyerEmail, testpassword)
        UserTestsHelper.adminCreateUser(testSellerEmail, testpassword)
       
        val r = UserTestsHelper.authUser(testSellerEmail, testpassword)
        val jwtTokenOfTheSeller = r.auth.get.jwt
        
        val jwtTokenOfTheBuyer = UserTestsHelper.authUser(testBuyerEmail, testpassword).auth.get.jwt

        val address = Address("500 hickman Rd", "Waukee", "50263", IA)
        val responseForApplication = UserTestsHelper.applyUserToBecomeSeller(jwtTokenOfTheSeller, address)
        UserTestsHelper.addProductToStore("BrownOysterMushrooms", jwtTokenOfTheSeller, Amount(100, USD))

        val products = UserTestsHelper.getProductsFromStore(jwtTokenOfTheSeller)
        val productsWithQuantity = products.listOfProduce.map(prd => ProductAndQuantity(prd, 1))

        val storeOrderRequestBody = StoreOrderRequestBody(productsWithQuantity)

        val responseFromCreatingArequest = UserTestsHelper.createStoreOrderRequest(jwtTokenOfTheBuyer, testSellerEmail, storeOrderRequestBody)

        
        assert(responseFromCreatingArequest.storeOrderRequest.seller.equals(Email(testSellerEmail)))
        assert(responseFromCreatingArequest.storeOrderRequest.buyer.equals(Email(testBuyerEmail)))
      }
    }
  }
}
