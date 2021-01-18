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

object UserTestSpec extends TestSuite {
  val tests = Tests {
    test("User Routes") {
      test("Should register a user") {

        val testEmail = "shanedrafahl@gmail.com"
        val testPassword = "teST12$5jljasdf"

        val uriToDeleteUser =
          Uri.fromString(s"/user/delete/${testEmail}").toOption.get
        val requestToDelteUser = Request[IO](Method.DELETE, uriToDeleteUser)

        TestUserRoutes
          .userRoutes[IO]
          .orNotFound(requestToDelteUser)
          .attempt
          .unsafeRunSync()

        val registrationArgs = s"${testEmail}/${testPassword}"

        val uriArg =
          Uri.fromString(s"/user/signup/$registrationArgs").toOption.get

        val regTestReq = Request[IO](Method.POST, uriArg)

        val responseFromCreatingUser = UserRoutes
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

        val expectedUserCreatedResponse = UserCreationRespose(
          "User Request Made: CodeDeliveryDetailsType(Destination=s***@g***.com, DeliveryMedium=EMAIL, AttributeName=email)",
          true
        )
        println(responseFromCreatingUser)
        assert(responseFromCreatingUser equals expectedUserCreatedResponse)
      }
    }
  }
}
