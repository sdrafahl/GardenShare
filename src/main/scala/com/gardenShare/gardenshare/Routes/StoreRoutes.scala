package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.UserEntities.JWTValidationTokens
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser._
import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT._
import cats.implicits._
import com.gardenShare.gardenshare.UserEntities.InvalidToken
import com.gardenShare.gardenshare.UserEntities.ValidToken
import com.gardenShare.gardenshare.domain.Store.Address
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.domain.Store.CreateStoreRequest
import com.gardenShare.gardenshare.Storage.Relational.InsertStore.CreateStoreRequestOps
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import scala.util.Try
import com.gardenShare.gardenshare.GoogleMapsClient.Distance
import com.gardenShare.gardenshare.GetNearestStores.GetNearestOps
import com.gardenShare.gardenshare.Helpers._
import cats.Applicative
import com.gardenShare.gardenshare.ProcessAndJsonResponse
import com.gardenShare.gardenshare.ProcessAndJsonResponse._
import com.gardenShare.gardenshare.domain.ProcessAndJsonResponse.ProcessData
import com.gardenShare.gardenshare.domain.Store.Store
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers._

object StoreRoutes {
  def storeRoutes[F[_]: Async: com.gardenShare.gardenshare.SignupUser.SignupUser: AuthUser: AuthJWT: InsertStore: GetNearestStores: GetDistance: GetStoresStream]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "store" / "create" / address => {
        addJsonHeaders(
          parseJWTokenFromRequest(req)
            .map(_.auth)
            .map{resultOfValidation =>
              resultOfValidation.flatMap{
                case InvalidToken(msg) => Applicative[F].pure(InvalidToken(msg).asJson)
                case ValidToken(Some(email)) => {
                  val addressOfSeller = Address(address)
                  val emailOfSeller = Email(email)
                  val request = CreateStoreRequest(addressOfSeller, Email(email))

                  ProcessData(
                    List(request).insertStore,
                    (lst: List[Store]) => StoresAdded(lst),
                    (err:Throwable) => FailedToAddStore(err.getMessage()),                    
                  ).process

                }
                case ValidToken(None) => Applicative[F].pure(InvalidToken("Token is valid but without email").asJson)
              }
            }
            .left
            .map(n => Applicative[F].pure(n.asJson))
            .fold(a1 => a1, a2 => a2)
            .map(_.toString())
            .flatMap(msg => Ok(msg)))
      }
      case req @ GET -> Root / "store" / address / limit / rangeInSeconds => {

        val maybeLimit = Try(limit.toInt).toEither.left
          .map(a => InvalidLimitProvided(a.getMessage()))

        val maybeRange = Try(rangeInSeconds.toFloat).toEither.left
          .map(a => InvalidRangeProvided(a.getMessage()))

        addJsonHeaders(maybeLimit.map{limit =>
          maybeRange.map{range =>
            parseJWTokenFromRequest(req)
              .map(_.auth)
              .map{resultOfValidation =>
                resultOfValidation.flatMap{
                  case InvalidToken(msg) => Applicative[F].pure(InvalidToken(msg).asJson)
                  case ValidToken(None) => Applicative[F].pure(InvalidToken("Token is valid but without email").asJson)
                  case ValidToken(Some(email)) => {
                    ProcessData(
                      GetNearestStore(Distance(range), limit, Address(address)).nearest,
                      (lst: List[Store]) => StoresAdded(lst),
                      (err:Throwable) => FailedToFindStore(err.getMessage())
                    ).process
                  }
                }
              }.foldIntoJson
          }.foldIntoJson
        }.foldIntoJson
        .flatMap(js => Ok(js.toString())))
      }
    }
  }
}
