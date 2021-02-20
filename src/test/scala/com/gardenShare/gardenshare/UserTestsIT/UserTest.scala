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
import com.gardenShare.gardenshare.Storage.Relational.InsertStore

object UserTestSpec extends TestSuite {


  val tests = Tests {
    test("User Routes") {

      val testEmail = "shanedrafahl@gmail.com"
      val testPassword = "teST12$5jljasdf"
      test("/user/signup/shanedrafahl@gmail.com/teST12$5jljasdf") {
        test("Should register a user") {
          UserTestsHelper.deleteUserAdmin(testEmail)                          
          val responseFromCreatingUser = UserTestsHelper.createUser(testEmail, testPassword)
          val expectedUserCreatedResponse = UserCreationRespose(
            "User Request Made: CodeDeliveryDetailsType(Destination=s***@g***.com, DeliveryMedium=EMAIL, AttributeName=email)",
            true
          )
          assert(responseFromCreatingUser equals expectedUserCreatedResponse)
        }
      }
      test("/user/auth/shanedrafahl@gmail.com/teST12$5jljasdf") {
        test("Should authenticate a valid user") {
          UserTestsHelper.deleteUserAdmin(testEmail)
          UserTestsHelper.adminCreateUser(testEmail, testPassword)
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          assert(r.auth.isDefined)
          assert(r.msg equals "jwt toke is valid")
        }
      }
      test("/user/jwt/") {
        test("Should authenticate a value JWT token") {
          UserTestsHelper.deleteUserAdmin(testEmail)
          UserTestsHelper.adminCreateUser(testEmail, testPassword)
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          val jwtToken = r.auth.get.jwt
          val authResponse = UserTestsHelper.authToken(jwtToken)
          assert(authResponse.msg equals "Token is valid")
        }
      }
      test("/user/apply-to-become-seller") {
        test("Should create application to become a seller"){
          UserTestsHelper.deleteUserAdmin(testEmail)
          UserTestsHelper.deletestore(Email(testEmail))
          UserTestsHelper.adminCreateUser(testEmail, testPassword)          
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          val jwtToken = r.auth.get.jwt
          val address = Address("500 hickman Rd", "Waukee", "50263", IA)
          val responseForApplication = UserTestsHelper.applyUserToBecomeSeller(jwtToken, address)
          val expectedSellerResponse = ResponseBody("User is now a seller", true)
          assert(responseForApplication equals expectedSellerResponse)
          val info = UserTestsHelper.getUserInfo(jwtToken)
          val expectedInfo = UserInfo(Email(testEmail), Sellers, Some(Store(info.store.get.id, address, Email(testEmail))))
          assert(info equals expectedInfo)
          val store = UserTestsHelper.getStore(Email(testEmail))
          assert(store.address equals address)
        }
      }
    }
  }
 }

object UserTestsHelper {

  /**
    Do Not Use in production
    */
  def deleteUserAdmin(email: String) = {
    val uriToDeleteUser =
      Uri.fromString(s"/user/delete/${email}").toOption.get
    val requestToDelteUser = Request[IO](Method.DELETE, uriToDeleteUser)

    TestUserRoutes
      .userRoutes[IO]
      .orNotFound(requestToDelteUser)
      .attempt
      .unsafeRunSync()
  }

  def createUser(email: String, password: String) = {
    val registrationArgs = s"${email}/${password}"

    val uriArg =
      Uri.fromString(s"/user/signup/$registrationArgs").toOption.get

    val regTestReq = Request[IO](Method.POST, uriArg)

    UserRoutes
      .userRoutes[IO]
      .orNotFound(regTestReq)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, UserCreationRespose])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def adminCreateUser(email: String, password: String) = {
    val registrationArgs = s"${email}/${password}"

    val uriArg =
      Uri.fromString(s"/user/$registrationArgs").toOption.get

    val regTestReq = Request[IO](Method.POST, uriArg)

    TestUserRoutes
      .userRoutes[IO]
      .orNotFound(regTestReq)
      .unsafeRunSync()
  }

  def authUser(email: String, password: String) = {
    val registrationArgs = s"${email}/${password}"

    val uriArg =
      Uri.fromString(s"/user/auth/$registrationArgs").toOption.get

    val regTestReq = Request[IO](Method.GET, uriArg)

    UserRoutes
      .userRoutes[IO]
      .orNotFound(regTestReq)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, AuthUserResponse])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def authToken(jwtToken: String) = {
    val uriArg = Uri.fromString(s"/user/jwt/${jwtToken}").toOption.get

    val authRequest = Request[IO](Method.GET, uriArg)

    UserRoutes
      .userRoutes[IO]
      .orNotFound(authRequest)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, IsJwtValidResponse])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def applyUserToBecomeSeller(jwt: String, a: Address): ResponseBody = {
    val uriArg = Uri.fromString("/user/apply-to-become-seller").toOption.get
    val headers = Headers.of(Header("authentication", jwt))

    val request = Request[IO](Method.POST, uriArg, headers = headers).withEntity(a.asJson.toString())


    UserRoutes
        .userRoutes[IO]
        .orNotFound(request)
        .unsafeRunSync()
        .body
        .through(text.utf8Decode)
        .through(stringArrayParser)
        .through(decoder[IO, ResponseBody])
        .compile
        .toList
        .unsafeRunSync()
        .head
    
  }

  def getUserInfo(jwt: String): UserInfo = {
    val uriArg = Uri.fromString("/user/info").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val infoRequest = Request[IO](Method.GET, uriArg, headers = headers)

    UserRoutes
      .userRoutes[IO]
      .orNotFound(infoRequest)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, UserInfo])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def deletestore(email: Email)(implicit d:DeleteStore[IO]) = d.delete(email).unsafeRunSync()
  def addStore(s: CreateStoreRequest)(implicit i:InsertStore[IO]) = i.add(List(s))
  def getStore(email: Email)(implicit d:GetStore[IO]) = d.getStoresByUserEmail(email).unsafeRunSync().head

  def getStores(limit: Int, rangeInSeconds: Int, jwt: String, address: Address) = {
    val uriArg = Uri.fromString(s"/store/${limit}/${rangeInSeconds}").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val storeRequest = Request[IO](Method.POST, uriArg, headers = headers).withEntity(address.asJson.toString())

    StoreRoutes
      .storeRoutes[IO]
      .orNotFound(storeRequest)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, NearestStores])
      .compile
      .toList
      .unsafeRunSync()
      .head     
  }
}
