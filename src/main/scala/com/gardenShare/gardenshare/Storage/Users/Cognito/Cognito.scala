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
import cats.effect.Async
import scala.util.Try
import scala.util.Success
import scala.util.Failure

object Cognito {
  implicit lazy val client = CognitoIdentityProviderClient.builder().build()

  def createUserPool[F[_]: Async](userPoolName: String)(implicit client: CognitoIdentityProviderClient) = {
    Async[F].async { (cb: Either[Throwable, CreateUserPoolResponse] => Unit) =>
      val response = Try(client.createUserPool(CreateUserPoolRequest.builder().poolName(userPoolName).build()))
      response match {
        case Success(response) => cb(Right(response))
        case Failure(error) => cb(Left(error))
      }
    }   
  }

  def createUserPoolClient[F[_]: Async](clientName: String, userPoolId: String)(implicit client: CognitoIdentityProviderClient) = {
    Async[F].async { (cb: Either[Throwable, UserPoolClientType] => Unit) =>
      val response = Try(client.createUserPoolClient(CreateUserPoolClientRequest.builder().clientName(clientName).userPoolId(userPoolId).build()))
      response match {
        case Success(resp) => cb(Right(resp.userPoolClient()))
        case Failure(error) => cb(Left(error))
      }
    }
  }

  def createUser[F[_]: Async](userName: String)(implicit client: CognitoIdentityProviderClient) = {
    Async[F].async {
      val maybeResponse = Try(client.adminCreateUser(AdminCreateUserRequest.builder().username(userName).build()))
      maybeResponse match {
        case Success(resp) => cb(Right(resp))
        case Failure(error) => cb(Left(error))
      }
    }
  }
}
