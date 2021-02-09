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

object StoreTest extends TestSuite {
  val email = "gardenShare@gmail.com"
  val password = "testPass12$"
  val tests = Tests {
    test("Product Routes") {
      test("store/10/60000") {

        val address0 = Address("902 24th St", "Des Moines", "50312", IA)
        val email0 = Email("shane0@gmail.com")

        val address1 = Address("907 58th St", "Des Moines", "50312", IA)
        val email1 = Email("shane1@gmail.com")

        val address2 = Address("911 Crocker St", "Des Moines", "50312", IA)
        val email2 = Email("shane2@gmail.com")

        val address3 = Address("912 14th St", "West Des Moines", "50265", IA)
        val email3 = Email("shane3@gmail.com")

        UserTestsHelper.deletestore(email0)
        UserTestsHelper.deletestore(email1)
        UserTestsHelper.deletestore(email2)
        UserTestsHelper.deletestore(email3)

        UserTestsHelper.deleteUserAdmin(email)
        UserTestsHelper.adminCreateUser(email, password)
        val r = UserTestsHelper.authUser(email, password)
        val jwtToken = r.auth.get.jwt
        
        UserTestsHelper.addStore(CreateStoreRequest(address0, email0)).unsafeRunSync()
        UserTestsHelper.addStore(CreateStoreRequest(address1, email1)).unsafeRunSync()
        UserTestsHelper.addStore(CreateStoreRequest(address2, email2)).unsafeRunSync()
        UserTestsHelper.addStore(CreateStoreRequest(address3, email3)).unsafeRunSync()

        val limit = 10
        val range = 5000000
        val addressFrom = Address("901 24th St", "Des Moines", "50312", IA)


        val result = UserTestsHelper.getStores(limit, range, jwtToken, addressFrom)
        println(result)
        val stores = result.store.map(s => Store(0, s.store.address, s.store.sellerEmail))

        val expectedStores = List(
          Store(0,Address("907 58th St","Des Moines","50312",IA),Email("shane1@gmail.com")),
          Store(0,Address("902 24th St","Des Moines","50312",IA),Email("shane0@gmail.com")),
          Store(0,Address("911 Crocker St","Des Moines","50312",IA),Email("shane2@gmail.com")),
          Store(0,Address("912 14th St","West Des Moines","50265",IA),Email("shane3@gmail.com"))
        )

        val correct = stores.map(a => expectedStores.contains(a))

        assert(correct.reduce((a,b) => a || b))
      }
    }
  }
}
