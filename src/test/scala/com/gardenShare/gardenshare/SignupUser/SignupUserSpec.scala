package com.gardenShare.gardenshare.SignupUser

import utest._
import cats.effect.IO
import com.gardenShare.gardenshare.CreateGarden.CreateGarden
import com.gardenShare.gardenshare.Storage.Relational.InsertGarden
import com.gardenShare.gardenshare.Storage.Relational.Gardens._
import com.gardenShare.gardenshare.Storage.Relational.Plants._
import com.gardenShare.gardenshare.Storage.Relational.InsertPlant
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import com.gardenShare.gardenshare.Config.GetUserPoolName

object SignupUserSpec extends TestSuite {
  val tests = Tests {
    test("SignupUser") {
      test("For IO") {
        test("signupUser") {
          test("Should use a cognito client to make a user request") {            
            val email = Email("me@me.com")
            val password = Password("testPass")

            val fakeSignupResponse = SignUpResponse.builder().build()

            implicit val userPoolName = GetUserPoolName[IO]
            implicit val mockCognitoClient = new CogitoClient[IO] {
              def createUserPool(userPoolName: String) = ???
              def createUserPoolClient(clientName: String, userPoolId: String) = ???
              def adminCreateUser(userName: String) = ???
              def createUser(givenPass: String, givenEmail: String): IO[SignUpResponse] = {
                assert(password equals Password(givenPass))
                assert(email equals Email(givenEmail))
                IO(fakeSignupResponse)
              }
            }

            val signUpUser = SignupUser[IO]()

            signUpUser.signupUser(email, password).unsafeRunSync()
          }
        }
      }
    }
  }
}
