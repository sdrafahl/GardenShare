package com.gardenShare.gardenshare.authenticateUser.AuthUser

import utest._
import cats.effect.IO
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser._
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import com.gardenShare.gardenshare.SignupUser.SignupUser
import com.gardenShare.gardenshare.SignupUser.SignupUser._
import com.gardenShare.gardenshare.UserEntities.UserResponse
import com.gardenShare.gardenshare.UserEntities.AuthenticatedUser
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetUserPoolId
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse
import com.gardenShare.gardenshare.Config.UserPoolName
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import com.gardenShare.gardenshare.Config.UserPoolID
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.UserEntities.JWTValidationTokens

object AuthUserSpec extends TestSuite {
  val tests = Tests {
    test("AuthUser") {
      test("for IO") {
        test("authTheUser") {
          test("Should authenticate a user") {
            val testUserPoolId = "testUserPoolId"
            val clientId = "clientId"

            implicit val testGetUserPoolName = new GetUserPoolName[IO] {
              def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[UserPoolName] = {
                IO(UserPoolName("clientId"))
              }
            }

            implicit val testGetUserPoolId = new GetUserPoolId[IO] {
              def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[UserPoolID] = IO(UserPoolID("idToken"))
            }

            implicit val testCognitoClient = new CogitoClient[IO] {
              def createUserPool(userPoolName: String): IO[CreateUserPoolResponse] = ???
              def createUserPoolClient(clientName: String, userPoolId: String): IO[UserPoolClientType] = ???
              def adminCreateUser(userName: String): IO[AdminCreateUserResponse] = ???
              def createUser(password: String, email: String, userPoolName:UserPoolName): SignUpResponse = ???
              def authUserAdmin(user: User, userPoolId: String, clientId: String): IO[AdminInitiateAuthResponse] = {
                (user, userPoolId, clientId) match {
                  case (User(Email("test@email.com"), Password("Password123$")), "idToken", "clientId") => {
                    val authResult = AuthenticationResultType
                      .builder()
                      .idToken("idToken")
                      .accessToken("accessToken")
                      .build()
                    IO(AdminInitiateAuthResponse
                      .builder()
                      .authenticationResult(authResult)
                      .build()
                    )
                  }
                  case _ => IO.raiseError(new Throwable("error"))
                }
              }
            }

            val result = User(Email("test@email.com"), Password("Password123$"))
              .auth
              .unsafeRunSync()

            assert(result equals AuthenticatedUser(User(Email("test@email.com"),Password("Password123$")),"idToken","accessToken"))
          }
        }
      }
    }
  }
}
