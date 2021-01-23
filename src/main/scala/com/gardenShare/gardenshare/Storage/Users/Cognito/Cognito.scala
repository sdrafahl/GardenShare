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
import com.gardenShare.gardenshare.UserEntities._

abstract class CogitoClient[F[_]:GetUserPoolName:Async] {
  def createUserPool(userPoolName: String): F[CreateUserPoolResponse]
  def createUserPoolClient(clientName: String, userPoolId: String): F[UserPoolClientType]
  def adminCreateUser(userName: String): F[AdminCreateUserResponse]
  def createUser(password: String, email: String, userPoolName:UserPoolName): SignUpResponse
  def authUserAdmin(user: User, userPoolId: String, clientId: String): F[AdminInitiateAuthResponse]
  def adminDeleteUser(email: Email, userPoolId: UserPoolID): F[AdminDeleteUserResponse]
  def addUserToGroup(email: String, userPoolName:UserPoolName, group: UserType): F[AdminAddUserToGroupResponse]
}

object CogitoClient {
  implicit lazy val cognitoIdentityClient = CognitoIdentityProviderClient.builder().build()
  def apply[F[_]:GetUserPoolName:GetTypeSafeConfig:Async]()(implicit client: CognitoIdentityProviderClient): CogitoClient[F] = new CogitoClient[F] {
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

  def adminCreateUser(userName: String) = {
    Async[F].async { (cb: Either[Throwable, AdminCreateUserResponse] => Unit) =>
      val maybeResponse = Try(client.adminCreateUser(
        AdminCreateUserRequest
          .builder()
          .username(userName)
          .desiredDeliveryMediums(EMAIL)
          .build()))
      maybeResponse match {
        case Success(resp) => cb(Right(resp))
        case Failure(error) => cb(Left(error))
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

    def addUserToGroup(email: String, userPoolName:UserPoolName, group: UserType): F[AdminAddUserToGroupResponse] = {
      val userRequest = AdminGetUserRequest
        .builder()
        .userPoolId(userPoolName.name)
        .username(email)
        .build()
      Async[F].async {cb =>
        group match {
        case Seller() => {          
          val req = AdminAddUserToGroupRequest
            .builder()
            .groupName("Sellers")
            .username(email)
            .userPoolId(userPoolName.name)
            .build()

          cb(Try(client.adminAddUserToGroup(req)).toEither) 
        }
        case _ => {
          cb(Left(new Throwable("Group Not Implemented")))
        }
        }
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
