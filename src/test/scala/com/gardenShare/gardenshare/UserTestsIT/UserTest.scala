package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Email
import utest.TestSuite
import utest.test
import utest.Tests
import com.gardenShare.gardenshare.UserInfo
import com.gardenShare.gardenshare.Sellers
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.IA
import java.net.URL
import eu.timepit.refined.auto._
import ParsingDecodingImplicits._

object UserTestSpec extends TestSuite {  

  import UserTestsHelper._

  val tests = Tests {
    test("User Routes") {

      val testEmail = Email("shanedrafahl@gmail.com")
      val testPassword = "teST12$5jljasdf"
      val testRefreshURL = new URL("http://localhost:3000/")
      val testReturnURL = new URL("http://localhost:3000/")
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
          UserTestsHelper.clearSlickAccounts(List(testEmail))
          UserTestsHelper.deleteUserAdmin(testEmail)
          UserTestsHelper.deletestore(testEmail)
          UserTestsHelper.adminCreateUser(testEmail, testPassword)          
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          val jwtToken = r.auth.get.jwt
          val address = Address("500 hickman Rd", "Waukee", "50263", IA)
          val responseForApplication = UserTestsHelper.applyUserToBecomeSeller(jwtToken,ApplyUserToBecomeSellerData(address,testRefreshURL, testReturnURL ))
          UserTestsHelper.verifyUserAsSeller(jwtToken, address)
          assert(!responseForApplication.head.url.toString().isEmpty())          
        }
      }
      test("/user/verify-user-as-seller") {
        test("Should create application to become a seller") {

          val testEmailSlickAccount = Email("gardensharetest@gmail.com")
          val testPass = "testPass12$"
          val accountID = UserTestsHelper.getTestStripeAccount

          UserTestsHelper.deleteUserAdmin(testEmailSlickAccount)
          UserTestsHelper.deletestore(testEmailSlickAccount)
          UserTestsHelper.deleteSlickEmailReference(testEmailSlickAccount).unsafeRunSync()

          UserTestsHelper.adminCreateUser(testEmailSlickAccount, testPass)
          UserTestsHelper.insertSlickEmailReference(testEmailSlickAccount, accountID).unsafeRunSync()

          val r = UserTestsHelper.authUser(testEmailSlickAccount, testPass)
          val jwtToken = r.auth.get.jwt
          val address = Address("501 hickman Rd", "Waukee", "50263", IA)

          val response = UserTestsHelper.verifyUserAsSeller(jwtToken, address).head
          val expectedResponse = ResponseBody("User is now a seller and address was set", true)
          assert(response.equals(expectedResponse))

          val info = UserTestsHelper.getUserInfo(jwtToken)
          val expectedInfo = UserInfo(testEmailSlickAccount, Sellers, Some(Store(info.store.get.id, address, testEmailSlickAccount)))
          assert(info equals expectedInfo)
          val store = UserTestsHelper.getStore(testEmailSlickAccount)
          assert(store.address equals address)
        }
      }
    }
  }
}

