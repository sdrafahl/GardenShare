package com.gardenShare.gardenshare.SignupUser

import utest._
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.InsertGarden
import com.gardenShare.gardenshare.Storage.Relational.Gardens._
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
import com.gardenShare.gardenshare.Config.UserPoolName
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse
import com.gardenShare.gardenshare.UserEntities._
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse
import com.gardenShare.gardenshare.Config.UserPoolID
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse

object SignupUserSpec extends TestSuite {
  val tests = Tests {
    test("SignupUser") {
      test("For IO") {
        test("signupUser") {
          test("Should use a cognito client to make a user request") {            
            val correctEmail = Email("shanedrafahl@gmail.com")
            val correctPassword = Password("testPass1*")
            val testUserPoolName = UserPoolName("userPool")

            implicit val testGetUserPoolName = new GetUserPoolName[IO] {
              def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[UserPoolName] = IO(testUserPoolName)
            }

            val fakeSignupResponse = SignUpResponse.builder().build()

            implicit val mockCognitoClient = new CogitoClient[IO] {
              def createUserPool(userPoolName: String) = ???
              def createUserPoolClient(clientName: String, userPoolId: String) = ???
              def adminCreateUser(userName: Email, password: Password, userPoolId: UserPoolID, clientId: String): IO[AdminRespondToAuthChallengeResponse] = ???
              
              def adminDeleteUser(email: Email, userPoolId: UserPoolID): IO[AdminDeleteUserResponse] = ???
              def addUserToGroup(email: String,userPoolId: UserPoolID,usertype: String): IO[AdminAddUserToGroupResponse] = ???
              def authUserAdmin(user: User, userPoolId: String, clientId: String): IO[AdminInitiateAuthResponse] = ???
              def listGroupsForUser(email: String, userPoolId: UserPoolID): IO[AdminListGroupsForUserResponse] = ???
              def createUser(password: String, email: String, userPoolName:UserPoolName): SignUpResponse = {
                assert(correctPassword equals Password(password))
                assert(correctEmail equals Email(email))
                assert(userPoolName equals testUserPoolName)
                fakeSignupResponse
              }
            }

            val signUpUser = SignupUser[IO]()

            val result = signUpUser.signupUser(correctEmail, correctPassword).unsafeRunSync()
            assert(result equals fakeSignupResponse)
          }
        }
      }
    }
  }
}
