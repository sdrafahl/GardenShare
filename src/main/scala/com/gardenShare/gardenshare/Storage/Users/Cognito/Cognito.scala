package com.gardenShare.gardenshare.Storage.Users.Cognito

import software.amazon.awssdk.regions.Region
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.OAuthFlowType
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType._
import cats.effect.Async
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.gardenShare.gardenshare.Config.GetUserPoolName
import software.amazon.awssdk.services.appsync.model.GetTypeRequest
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import cats.syntax.functor._
import cats.effect.IO
import com.gardenShare.gardenshare.Config.UserPoolSecret
import com.gardenShare.gardenshare.Config.UserPoolName
import com.gardenShare.gardenshare.UserEntities.User
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType._
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AddCustomAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.SchemaAttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeDataType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupResponse
import com.gardenShare.gardenshare.UserEntities.Email
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse
import com.gardenShare.gardenshare.Config.UserPoolID
import com.gardenShare.gardenshare.UserEntities.Password
import cats.implicits._
import cats.FlatMap
import com.amazonaws.auth.AnonymousAWSCredentials
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse
import com.gardenShare.gardenshare.UserEntities.UserType
import com.gardenShare.gardenshare.UserEntities.InvalidType

abstract class CogitoClient[F[_]:GetUserPoolName:Async:FlatMap] {
  def createUserPool(userPoolName: String): F[CreateUserPoolResponse]
  def createUserPoolClient(clientName: String, userPoolId: String): F[UserPoolClientType]
  def adminCreateUser(userName: Email, password: Password, userPoolId: UserPoolID, clientId: String): F[AdminRespondToAuthChallengeResponse]
  def createUser(password: String, email: String, userPoolName:UserPoolName): SignUpResponse
  def authUserAdmin(user: User, userPoolId: String, clientId: String): F[AdminInitiateAuthResponse]
  def adminDeleteUser(email: Email, userPoolId: UserPoolID): F[AdminDeleteUserResponse]
  def addUserToGroup(email: String, userPoolName:UserPoolName, usertype: String): F[AdminAddUserToGroupResponse]
}

object CogitoClient {
  implicit lazy val cognitoIdentityClient = CognitoIdentityProviderClient.builder().build()
  def apply[F[_]:GetUserPoolName:GetTypeSafeConfig:Async:FlatMap]()(implicit client: CognitoIdentityProviderClient): CogitoClient[F] = new CogitoClient[F] {
    def createUserPool(userPoolName: String) = {
    Async[F].async { (cb: Either[Throwable, CreateUserPoolResponse] => Unit) =>
      val response = Try(client.createUserPool(
        CreateUserPoolRequest
          .builder()
          .poolName(userPoolName)
          .build())
      )
      response match {
        case Success(response) => cb(Right(response))
        case Failure(error) => cb(Left(error))
      }
    }
    }

  def createUserPoolClient(clientName: String, userPoolId: String) = {
    Async[F].async { (cb: Either[Throwable, UserPoolClientType] => Unit) =>
      val response = Try(client.createUserPoolClient(CreateUserPoolClientRequest
        .builder()
        .clientName(clientName)
        .userPoolId(userPoolId)
        .build()))
      response match {
        case Success(resp) => cb(Right(resp.userPoolClient()))
        case Failure(error) => cb(Left(error))
      }
    }
  }

    def adminCreateUser(userName: Email, password: Password, userPoolId: UserPoolID, clientId: String) = {

      val tempPassword = "tempPassword123$$"

      val userCreated = Async[F].async { (cb: Either[Throwable, AdminCreateUserResponse] => Unit) =>
        

        val maybeResponse = Try(client.adminCreateUser(        
          AdminCreateUserRequest
            .builder()
            .username(userName.underlying)
            .userPoolId(userPoolId.id)
            .temporaryPassword(tempPassword)
            .build()))

        maybeResponse match {
          case Success(resp) => cb(Right(resp))
          case Failure(error) => cb(Left(error))
        }
      }
      userCreated.flatMap{adminResp =>
        authUserAdmin(User(userName, Password(tempPassword)), userPoolId.id, clientId).flatMap{authResp =>

          val chngPassRequest = AdminRespondToAuthChallengeRequest
            .builder()
            .challengeName("NEW_PASSWORD_REQUIRED")
            .challengeResponses(Map(
              "NEW_PASSWORD" -> password.underlying,
              "USERNAME" -> userName.underlying
            ).asJava)
            .clientId(clientId)
            .userPoolId(userPoolId.id)
            .session(authResp.session())
            .build()

          Async[F].async { cb: (Either[Throwable, AdminRespondToAuthChallengeResponse] => Unit) =>
            cb(Try(client.adminRespondToAuthChallenge(chngPassRequest)).toEither)
          }
        }
      }
  }

  def createUser(password: String, email: String, userPoolName:UserPoolName) = {   
      client.signUp(
          SignUpRequest
            .builder()
            .password(password)
            .clientId(userPoolName.name)
            .username(email)
            .build()
      )
  }

    def addUserToGroup(email: String, userPoolName:UserPoolName, usertype: String): F[AdminAddUserToGroupResponse] = {
      Async[F].async {cb =>
        val req = AdminAddUserToGroupRequest
          .builder()
          .groupName(usertype)
          .username(email)
          .userPoolId(userPoolName.name)
          .build()
        cb(Try(client.adminAddUserToGroup(req)).toEither)
      }
    }

  def authUserAdmin(user: User, userPoolId: String, clientId: String): F[AdminInitiateAuthResponse] = {

      val params = Map(
          "USERNAME" -> user.email.underlying,
          "PASSWORD" -> user.password.underlying
      ).asJava


    val request = AdminInitiateAuthRequest
      .builder()
      .authFlow(ADMIN_NO_SRP_AUTH)
      .authParameters(params)
      .userPoolId(userPoolId)
      .clientId(clientId)
      .build()

      Async[F].async { cb =>
        cb(Try(client.adminInitiateAuth(request)).toEither)
      }
  }

  def adminDeleteUser(email: Email, userPoolId: UserPoolID): F[AdminDeleteUserResponse] = {
    val request = AdminDeleteUserRequest
      .builder()
      .userPoolId(userPoolId.id)
      .username(email.underlying)
      .build()

    Async[F].async { cb =>
      cb(Try(client.adminDeleteUser(request)).toEither)
    }
  }

  }
  implicit lazy val defaultCognitoClient: CogitoClient[IO] = CogitoClient[IO]()
}
