package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString

import cats.implicits._
import cats.effect.Async
import com.gardenShare.gardenshare.Helpers._

import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._

import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import cats.Applicative
import com.gardenShare.gardenshare.UserEntities.InvalidToken
import com.gardenShare.gardenshare.UserEntities.ValidToken
import com.gardenShare.gardenshare.CreateStoreOrderRequest
import cats.effect.ContextShift
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers.FoldOverIntoJsonOps
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers
import com.gardenShare.gardenshare.domain.ProcessAndJsonResponse.ProcessData
import com.gardenShare.gardenshare.UserEntities.Email
import cats.ApplicativeError
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser.AuthUserOps
import java.time.ZonedDateTime
import scala.util.Try
import scala.util.Failure
import scala.util.Success

case class StoreOrderRequestBody(body: List[ProductWithId])
case class StoreOrderRequestsBelongingToSellerBody(body: List[StoreOrderRequestWithId])

object StoreOrderRoutes {
  def storeOrderRoutes[F[_]: Async: ContextShift:CreateStoreOrderRequest:AuthUser: AuthJWT:GetCurrentDate:GetStoreOrderRequestsWithinTimeRangeOfSeller]
    (implicit ae: ApplicativeError[F, Throwable], pp: ProcessAndJsonResponse, en: Encoder[Produce], produceDecoder: Decoder[Produce], currencyEncoder: Encoder[Currency], currencyDecoder: Decoder[Currency]): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of {
      case req @ POST -> Root / "storeOrderRequest" / sellerEmail => {
        parseJWTokenFromRequest(req)
          .map(_.auth)
          .map{resultOfValidation =>
            resultOfValidation.flatMap{
              case InvalidToken(msg) => Applicative[F].pure(InvalidToken(msg).asJson)
              case ValidToken(None) => Applicative[F].pure(InvalidToken("Token is valid but without email").asJson)
              case ValidToken(Some(email)) => {
                parseBodyFromRequest[StoreOrderRequestBody, F](req).flatMap{
                  case None => Applicative[F].pure(ResponseBody("Cant parse product with ids", false).asJson)
                  case Some(products) => {
                    ProcessData(
                      implicitly[CreateStoreOrderRequest[F]].createOrder(Email(sellerEmail), Email(email), products.body),
                      (l: StoreOrderRequestWithId) => l,
                      (err: Throwable) => ResponseBody(s"Error creating store order request: ${err.getMessage()}", false)
                    ).process
                  }
                }
              }
            }
          }
          .foldIntoJson
          .flatMap(a => Ok(a.toString()))
          .catchError
      }
      case req @ GET -> Root / "storeOrderRequest" / "seller" / from / to => {
        ((Try(ZonedDateTime.parse(from)), Try(ZonedDateTime.parse(to))) match {
          case (Failure(a), _) => Applicative[F].pure(ResponseBody("From date is not valid zone date format", false).asJson)
          case (_, Failure(a)) => Applicative[F].pure(ResponseBody("To date is not valid zone date format", false).asJson)
          case (Success(fromDateZone), Success(toDateZone)) => {
             parseJWTokenFromRequest(req)
              .map(_.auth)
              .map{resultOfValidation =>
                resultOfValidation.flatMap{
                  case InvalidToken(msg) => Applicative[F].pure(InvalidToken(msg).asJson)
                  case ValidToken(None) => Applicative[F].pure(InvalidToken("Token is valid but without email").asJson)
                  case ValidToken(Some(email)) => {
                    ProcessData(
                      implicitly[GetStoreOrderRequestsWithinTimeRangeOfSeller[F]].getStoreOrdersWithin(fromDateZone, toDateZone, Email(email)),
                      (l: List[StoreOrderRequestWithId]) => StoreOrderRequestsBelongingToSellerBody(l),
                      (err: Throwable) => ResponseBody(s"Error getting list of store order requests: ${err.getMessage()}", false)
                    ).process
                  }
                }
              }.foldIntoJson
          }
        }).flatMap(a => Ok(a.toString()))
          .catchError
      }
    }
  }
}
