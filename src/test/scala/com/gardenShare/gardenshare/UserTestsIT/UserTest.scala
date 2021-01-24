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
import com.gardenShare.gardenshare.domain.SellerResponse
import com.gardenShare.gardenshare.domain.SellerRequestSuccessful
import com.gardenShare.gardenshare.domain.User.UserInfo
import com.gardenShare.gardenshare.UserEntities.Sellers
import com.gardenShare.gardenshare.Encoders.Encoders._

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
          assert(r.msg equals "jwt token is valid")
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
          UserTestsHelper.adminCreateUser(testEmail, testPassword)
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          val jwtToken = r.auth.get.jwt
          val responseForApplication = UserTestsHelper.applyUserToBecomeSeller(jwtToken)
          val expectedSellerResponse = SellerRequestSuccessful()
          assert(responseForApplication equals expectedSellerResponse)
          val info = UserTestsHelper.getUserInfo(jwtToken)
          val expectedInfo = UserInfo(Email(testEmail), Sellers)
          assert(info equals expectedInfo)
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

  def applyUserToBecomeSeller(jwt: String): SellerResponse = {
    val uriArg = Uri.fromString("/user/apply-to-become-seller").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val applicationRequest = Request[IO](Method.POST, uriArg, headers = headers)

    UserRoutes
      .userRoutes[IO]
      .orNotFound(applicationRequest)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, SellerResponse])
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

}
