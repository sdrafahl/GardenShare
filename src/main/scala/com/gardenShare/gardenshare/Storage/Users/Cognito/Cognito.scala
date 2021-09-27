package com.gardenShare.gardenshare

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import cats.effect.Async
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import com.gardenShare.gardenshare.UserPoolName
import com.gardenShare.gardenshare.User
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType._
import scala.jdk.CollectionConverters._
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupResponse
import com.gardenShare.gardenshare.Email
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserResponse
import com.gardenShare.gardenshare.UserPoolID
import com.gardenShare.gardenshare.Password
import cats.implicits._
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupResponse

abstract class CogitoClient[F[_]] {
  def createUserPool(userPoolName: String): F[CreateUserPoolResponse]
  def createUserPoolClient(
    clientName: String,
    userPoolId: String
  ): F[UserPoolClientType]
  def adminCreateUser(
    userName: Email,
    password: Password,
    userPoolId: UserPoolID,
    clientId: String
  ): F[AdminRespondToAuthChallengeResponse]
  def createUser(
    password: String,
    email: String,
    userPoolName: UserPoolName
  ): SignUpResponse
  def authUserAdmin(
    user: User,
    userPoolId: String,
    clientId: String
  ): F[AdminInitiateAuthResponse]
  def adminDeleteUser(
    email: Email,
    userPoolId: UserPoolID
  ): F[AdminDeleteUserResponse]
  def addUserToGroup(
    email: String,
    userPoolId: UserPoolID,
    usertype: String
  ): F[AdminAddUserToGroupResponse]
  def listGroupsForUser(
    email: String,
    userPoolId: UserPoolID
  ): F[AdminListGroupsForUserResponse]
}

object CogitoClient {

  implicit def apply[F[_]: Async](
      implicit client: CognitoIdentityProviderClient = CognitoIdentityProviderClient.builder().build()
  ): CogitoClient[F] = new CogitoClient[F] {
    def createUserPool(userPoolName: String) = {
      Async[F].async_ {
        (cb: Either[Throwable, CreateUserPoolResponse] => Unit) =>
          val response = Try(
            client.createUserPool(
              CreateUserPoolRequest
                .builder()
                .poolName(userPoolName)
                .build()
            )
          )
          response match {
            case Success(response) => cb(Right(response))
            case Failure(error)    => cb(Left(error))
          }
      }
    }

    def createUserPoolClient(clientName: String, userPoolId: String) = {
      Async[F].async_ { (cb: Either[Throwable, UserPoolClientType] => Unit) =>
        val response = Try(
          client.createUserPoolClient(
            CreateUserPoolClientRequest
              .builder()
              .clientName(clientName)
              .userPoolId(userPoolId)
              .build()
          )
        )
        response match {
          case Success(resp)  => cb(Right(resp.userPoolClient()))
          case Failure(error) => cb(Left(error))
        }
      }
    }

    def adminCreateUser(
        userName: Email,
        password: Password,
        userPoolId: UserPoolID,
        clientId: String
    ) = {

      val tempPassword = "tempPassword123$$"

      val userCreated = Async[F].async_ {
        (cb: Either[Throwable, AdminCreateUserResponse] => Unit) =>
          val maybeResponse = Try(
            client.adminCreateUser(
              AdminCreateUserRequest
                .builder()
                .username(userName.underlying.value)
                .userPoolId(userPoolId.id)
                .temporaryPassword(tempPassword)
                .build()
            )
          )

          maybeResponse match {
            case Success(resp)  => cb(Right(resp))
            case Failure(error) => cb(Left(error))
          }
      }
      userCreated.flatMap {_ =>
        authUserAdmin(
          User(userName, Password(tempPassword)),
          userPoolId.id,
          clientId
        ).flatMap { authResp =>
          val chngPassRequest = AdminRespondToAuthChallengeRequest
            .builder()
            .challengeName("NEW_PASSWORD_REQUIRED")
            .challengeResponses(
              Map(
                "NEW_PASSWORD" -> password.underlying,
                "USERNAME" -> userName.underlying.value
              ).asJava
            )
            .clientId(clientId)
            .userPoolId(userPoolId.id)
            .session(authResp.session())
            .build()

          Async[F].async_ {
            cb: (Either[Throwable, AdminRespondToAuthChallengeResponse] => Unit) =>
              cb(
                Try(client.adminRespondToAuthChallenge(chngPassRequest)).toEither
              )
          }
        }
      }
    }

    def createUser(
        password: String,
        email: String,
        userPoolName: UserPoolName
    ) = {
      client.signUp(
        SignUpRequest
          .builder()
          .password(password)
          .clientId(userPoolName.name)
          .username(email)
          .build()
      )
    }

    def addUserToGroup(
        email: String,
        userPoolId: UserPoolID,
        usertype: String
    ): F[AdminAddUserToGroupResponse] = {
      Async[F].async_ { cb =>
        val req = AdminAddUserToGroupRequest
          .builder()
          .groupName(usertype)
          .username(email)
          .userPoolId(userPoolId.id)
          .build()
        cb(Try(client.adminAddUserToGroup(req)).toEither)
      }
    }

    def authUserAdmin(
        user: User,
        userPoolId: String,
        clientId: String
    ): F[AdminInitiateAuthResponse] = {

      val params = Map(
        "USERNAME" -> user.email.underlying.value,
        "PASSWORD" -> user.password.underlying
      ).asJava

      val request = AdminInitiateAuthRequest
        .builder()
        .authFlow(ADMIN_NO_SRP_AUTH)
        .authParameters(params)
        .userPoolId(userPoolId)
        .clientId(clientId)
        .build()

      Async[F].async_ { cb =>
        cb(Try(client.adminInitiateAuth(request)).toEither)
      }
    }

    def adminDeleteUser(
        email: Email,
        userPoolId: UserPoolID
    ): F[AdminDeleteUserResponse] = {
      val request = AdminDeleteUserRequest
        .builder()
        .userPoolId(userPoolId.id)
        .username(email.underlying.value)
        .build()

      val removeUserFromGroup = AdminRemoveUserFromGroupRequest
        .builder()
        .userPoolId(userPoolId.id)
        .username(email.underlying.value)
        .groupName("Sellers")
        .build()      

      val removeFromGroupPgm = Async[F]
        .async_[AdminRemoveUserFromGroupResponse]{ cb => cb(Try(client.adminRemoveUserFromGroup(removeUserFromGroup)).toEither)}        

      val deleteUserPgm = Async[F]
        .async_[AdminDeleteUserResponse] { cb => cb(Try(client.adminDeleteUser(request)).toEither) }

      removeFromGroupPgm >> deleteUserPgm
    }

    def listGroupsForUser(
        email: String,
        userPoolId: UserPoolID
    ): F[AdminListGroupsForUserResponse] = {
      val request = AdminListGroupsForUserRequest
        .builder()
        .username(email)
        .userPoolId(userPoolId.id)
        .build()

      Async[F].async_ { cb =>
        cb(Try(client.adminListGroupsForUser(request)).toEither)
      }
    }
  }
}
