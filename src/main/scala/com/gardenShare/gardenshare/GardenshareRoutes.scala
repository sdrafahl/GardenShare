package com.gardenShare.gardenshare

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.Encryption.Decrypt
import com.gardenShare.gardenshare.Config.GetPrivateKey
import org.apache.commons.codec.binary.Base64
import com.gardenShare.gardenshare.SignupUser.SignupUser._
import com.gardenShare.gardenshare.SignupUser.SignupUser
import cats.effect.IO
import cats.effect.Async
import com.gardenShare.gardenshare.UserEntities.User
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.Password
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Storage.Users.Cognito._
import com.gardenShare.gardenshare.SignupUser.SignupUser._
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient._
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetUserPoolSecret
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.UserEntities.AuthenticatedUser
import com.gardenShare.gardenshare.UserEntities.FailedToAuthenticate
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.Config.GetUserPoolId
import com.gardenShare.gardenshare.UserEntities.UserResponse
import io.circe.generic.auto._, io.circe.syntax._
import java.time.LocalDate
import com.gardenShare.gardenshare.UserEntities.JWTValidationTokens
import com.gardenShare.gardenshare.UserEntities.InvalidToken
import com.gardenShare.gardenshare.UserEntities.ValidToken
import com.gardenShare.gardenshare.Config.GetRegion
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.HttpsJwksBuilder
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT.AuthJwtOps
import org.http4s.dsl.impl.Responses.BadRequestOps
import io.circe.generic.auto._, io.circe.syntax._
import org.http4s.Header
import com.gardenShare.gardenshare.UserEntities.Group
import com.gardenShare.gardenshare.Encoders.Encoders._
import com.gardenShare.gardenshare.domain.Store.Address
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream._
import com.gardenShare.gardenshare.domain.Store.CreateStoreRequest
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import com.gardenShare.gardenshare.Storage.Relational.InsertStore.CreateStoreRequestOps
import com.gardenShare.gardenshare.domain.Store.Store
import scala.util.Try
import com.gardenShare.gardenshare.GetNearestStores
import com.gardenShare.gardenshare.GetNearestStore
import com.gardenShare.gardenshare.GoogleMapsClient.Distance
import com.gardenShare.gardenshare.GetNearestStores
import com.gardenShare.gardenshare.GetNearestStores.GetNearestOps
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream
import com.gardenShare.gardenshare.Storage.S3.GetKeys
import com.gardenShare.gardenshare.GetListOfProductNames.GetListOfProductNames
import com.gardenShare.gardenshare.Config.GetDescriptionBucketName
import com.gardenShare.gardenshare.GetListOfProductNames.DescriptionName
import com.gardenShare.gardenshare.Storage.Relational.InsertProduct
import com.gardenShare.gardenshare.domain.Products.CreateProductRequest
import com.gardenShare.gardenshare.domain.Products.DescriptionAddress
import com.gardenShare.gardenshare.Storage.Relational.GetProductsByStore
import cats.Applicative
import com.gardenShare.gardenshare.domain.Products.S3DescriptionAddress
import com.gardenShare.gardenshare.GetDescription.Ops
import com.gardenShare.gardenshare.domain.Products.ParseDescriptionAddress
import com.gardenShare.gardenshare.Storage.S3.ReadS3File

object GardenshareRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  case class ResponseBody(msg: String)
  case class UserCreationRespose(msg: String, userCreated: Boolean)
  case class AuthUserResponse(msg: String, auth: Option[AuthenticatedUser], authenticated: Boolean)
  case class IsJwtValidResponse(msg: String, valid: Boolean, groups: List[Group])

  def userRoutes[F[_]:
      Async:
      CogitoClient:
      GetUserPoolName:
      GetTypeSafeConfig:
      SignupUser:
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder:
      GetDistance
  ](): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case POST -> Root / "user" / "signup" / email / password => {
        val emailToPass = Email(email)
        val passwordToPass = Password(password)
        val user = User(emailToPass,passwordToPass)
        val processRequest = (for {
          resp <- user.signUp[F]()
        } yield resp)
          .attempt
        for {
          result <- processRequest
          newResp <- result match {
            case Left(err) =>  Ok(UserCreationRespose(s"User Request Failed: ${err.getMessage()}", false).asJson.toString())
            case Right(resp) => Ok(UserCreationRespose(s"User Request Made: ${resp.codeDeliveryDetails().toString()}", true).asJson.toString())
              .map(a => a.copy(headers = a.headers.put(Header.apply("Content-Type", "application/json"))))
          }
        } yield newResp
      }

      case GET -> Root / "user" / "auth" / email / password => {
        val result =  User(Email(email), Password(password))
          .auth
          .attempt

          result.flatMap{mr =>
            mr match {
              case Left(error) => Ok(AuthUserResponse(s"Error Occurred: ${error}", None, false).asJson.toString())
              case Right(AuthenticatedUser(user, jwt, accToken)) =>
                Ok(AuthUserResponse("jwt token is valid", Option(AuthenticatedUser(user, jwt, accToken)), true).asJson.toString())
                .map(a => a.copy(headers = a.headers.put(Header.apply("Content-Type", "application/json"))))
                
              case Right(FailedToAuthenticate(msg)) => {
                val response = AuthUserResponse(s"User failed to verify: ${msg}",None, false)
                Ok(response.asJson.toString())
              }
              case _ => NotAcceptable(ResponseBody(s"Unknown response").asJson.toString())
            }
          }
      }
      case GET -> Root / "user" / "jwt" / jwtToken => {
        val result = JWTValidationTokens(jwtToken)
          .auth[F]
          .attempt

          result.flatMap {rest =>
            rest match {
              case Left(error) => Ok(IsJwtValidResponse(s"Error occured: ${error}", false, List()).asJson.toString())
              case Right(ValidToken(email, userGroups)) => Ok(IsJwtValidResponse("Token is valid", true, userGroups).asJson.toString())
              case Right(InvalidToken(msg)) => Ok(IsJwtValidResponse("Token is not valid", false, List()).asJson.toString())
              case Right(_) => NotAcceptable(ResponseBody("Unknown response").asJson.toString())
            }
          }
      }
    }
  }

  case class NoJWTTokenProvided()
  case class StoresAdded(store: List[Store])
  case class InvalidLimitProvided(msg: String)
  case class InvalidRangeProvided(msg: String)
  case class ListOfStores(l: List[Store])

  def storeRoutes[F[_]:
      Async:
      CogitoClient:
      GetUserPoolName:
      GetTypeSafeConfig:
      SignupUser:
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder:
      InsertStore:
      GetNearestStores:
      GetDistance:
      GetStoresStream
  ]() : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "store" / "create" / address => {
        val key = CaseInsensitiveString("authentication")
        val maybeJwtHeader = req
          .headers
          .get(key)

        val token = maybeJwtHeader match {
          case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
          case None => Left(Ok(NoJWTTokenProvided().asJson.toString()))
        }

        token.map {f =>
          f
            .auth        
            .map {
              case InvalidToken(msg) => Ok(InvalidToken(msg).asJson.toString())
              case ValidToken(Some(email), _) => {
                val addressOfSeller = Address(address)
                val emailOfSeller = Email(email)
                val request = CreateStoreRequest(addressOfSeller, Email(email))
                List(request)
                  .insertStore
                  .map(st => Ok(StoresAdded(st).asJson.toString()))
                  .flatMap(a => a)
              }
              case ValidToken(None, _) => Ok(InvalidToken("Token is valid but without email").asJson.toString())
            }
            .flatMap(a => a)            
        }.fold(a => a, b => b)
      }
      case req @ GET -> Root / "store" / address / limit / rangeInSeconds => {

        val key = CaseInsensitiveString("authentication")

        val maybeJwtHeader = req
          .headers
          .get(key)

        val token = maybeJwtHeader match {
          case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
          case None => Left(Ok(NoJWTTokenProvided().asJson.toString()))
        }

        val lim = Try(limit.toInt)
          .toEither
          .left
          .map(a => InvalidLimitProvided(a.getMessage()))

        val range = Try(rangeInSeconds.toFloat)
          .toEither
          .left
          .map(a => InvalidRangeProvided(a.getMessage()))
         
          
        token.map {f =>
          f
            .auth        
            .map {
              case InvalidToken(msg) => Ok(InvalidToken(msg).asJson.toString())
              case ValidToken(Some(email), _) => {
                lim.map { li =>
                  range.map {range =>                    
                    GetNearestStore(Distance(range), li, Address(address))
                      .nearest
                      .map(ListOfStores)
                      .map(_.asJson.toString())
                      .attempt
                      .map(_.left.map(err => ResponseBody(err.toString()).asJson.toString()))
                      .map(_.map(msg => ResponseBody(msg).asJson.toString()))
                      .map(_.fold(a => a, b => b))
                      .map(respMsg => Ok(respMsg))
                      .flatMap(a => a)
                    
                  }
                    .fold(fa => Ok(fa.asJson.toString()), fb => fb)
                }
                  .fold(a => Ok(a.asJson.toString()), b => b)
              }
              case ValidToken(None, _) => Ok(InvalidToken("Token is valid but without email").asJson.toString())
            }
        }.map(_.flatMap(a => a))
         .fold(a => a, b => b)
      }
    }
  }

  case class InvalidDescriptionName()
  case class InvalidStoreIDInput()
  def productRoutes[F[_]:
      Async:
      CogitoClient:
      GetUserPoolName:
      GetTypeSafeConfig:
      SignupUser:
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder:
      InsertStore:
      GetNearestStores:
      GetDistance:
      GetStoresStream:
      GetListOfProductNames:
      GetKeys:
      GetDescriptionBucketName:
      InsertProduct:
      GetProductsByStore
  ]() : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "product" / "create" / storeId / descripKey => {
        val key = CaseInsensitiveString("authentication")
        val insertProduct = InsertProduct[F]()

        val maybeJwtHeader = req
          .headers
          .get(key)

        val storeIdentification = storeId.toIntOption match {
          case None => Left(Ok(InvalidStoreIDInput().asJson.toString()))
          case Some(a) => Right(a)
        }

        val token = maybeJwtHeader match {
          case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
          case None => Left(Ok(NoJWTTokenProvided().asJson.toString()))
        }

        val isValidDescProgram = GetListOfProductNames().getListOfProducts.map {(f: List[DescriptionName]) =>
          (f.contains(DescriptionName(descripKey)), token)
        }

        val currentError = isValidDescProgram.map {
          case (_, Left(err)) => Left(err)
          case (true, Right(l)) => Right(l)
          case (false, _) => Left(Ok(InvalidDescriptionName().asJson.toString()))
        }

        currentError.map { maybeToken =>
          maybeToken
            .map(token => token.auth)
            .map(a => a.map{
              case InvalidToken(msg) => Left(Ok(InvalidToken(msg).asJson.toString()))
              case ValidToken(maybeEmail, groups) => {
                storeIdentification.map { stoId =>
                  insertProduct.add(List(CreateProductRequest(stoId, DescriptionAddress(descripKey))))
                    .map(_.asJson.toString())
                    .flatMap(Ok(_))
                }                
              }
            })
        }.flatMap(ac =>
          ac.fold(b => b, c => c.flatMap(d => d.fold(e => e, f => f)))
        )
          .attempt
          .map(_.left.map(err => Ok(ResponseBody(err.getMessage()).asJson.toString())))
          .map(_.map(a => Applicative[F].pure(a)))
          .map(_.fold(a => a, b => b))
          .flatten
      }
      case req @ GET -> Root / "product" / storeId => {

        val getProducts = GetProductsByStore[F]()

        val storeIden = storeId.toIntOption match {
          case None => Left(Ok(InvalidStoreIDInput().asJson.toString()))
          case Some(a) => Right(a)
        }

        val key = CaseInsensitiveString("authentication")

        val maybeJwtHeader = req
          .headers
          .get(key)

        val token = maybeJwtHeader match {
          case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
          case None => Left(Ok(NoJWTTokenProvided().asJson.toString()))
        }

        storeIden.flatMap{ id =>
          token.map(_.auth).flatMap {
            case InvalidToken(msg) => Left(Ok(InvalidToken(msg).asJson.toString()))
            case ValidToken(maybeEmail, groups) => {
              Right(getProducts.getProductsByStore(id).flatMap(lst => Ok(lst.asJson.toString())))
            }
          }
        }
          .fold(a => a, b => b)
          .attempt
          .map(_.left.map(az => Ok(ResponseBody(az.getMessage()).asJson.toString())))
          .map(_.map(a => Applicative[F].pure(a)))
          .map(_.fold(a => a, b => b))
          .flatten
      }
    }
  }
  case class CantFindDescriptionBucketName(msg: String)
  def productDescriptionRoutes[F[_]:
      Async:
      CogitoClient:
      GetUserPoolName:
      GetTypeSafeConfig:
      SignupUser:
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder:
      InsertStore:
      GetNearestStores:
      GetDistance:
      GetStoresStream:
      GetListOfProductNames:
      GetKeys:
      GetDescriptionBucketName:
      InsertProduct:
      GetDescription:
      ReadS3File
  ]() : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "productDescription" / descKey => {
        GetDescriptionBucketName()
          .get
          .attempt
          .map(_.left.map(a => Ok(CantFindDescriptionBucketName(a.getMessage()).asJson.toString())))
          .map(_.map { bucketName => 
            val address = S3DescriptionAddress(bucketName.underlying, descKey)
            address
              .getDesc
              .map(_.asJson.toString())
              .attempt
              .map(_.left.map(a => ResponseBody(a.getMessage()).asJson.toString()))
              .map(_.fold(a => a, b => b))
              .map(Ok(_))
              .flatten
          })
          .map(_.fold(a => a, b => b))
          .flatten
      }
    }
  }
}
