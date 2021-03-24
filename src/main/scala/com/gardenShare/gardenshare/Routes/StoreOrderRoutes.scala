package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString

import cats.implicits._
import cats.effect.Async
import com.gardenShare.gardenshare.Helpers._

import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._

import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthJWT
import cats.Applicative
import com.gardenShare.gardenshare.InvalidToken
import com.gardenShare.gardenshare.ValidToken
import com.gardenShare.gardenshare.CreateStoreOrderRequest
import cats.effect.ContextShift
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers.FoldOverIntoJsonOps
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.Email
import cats.ApplicativeError
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import com.gardenShare.gardenshare.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.AuthUser.AuthUserOps
import java.time.ZonedDateTime
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContext

case class StoreOrderRequestBody(body: List[ProductAndQuantity])
case class StoreOrderRequestsBelongingToSellerBody(body: List[StoreOrderRequestWithId])
case class StoreOrderRequestStatusBody(response: StoreOrderRequestStatus)

object StoreOrderRoutes {
  def storeOrderRoutes[F[_]: Async: ContextShift:CreateStoreOrderRequest:AuthUser: AuthJWT:GetCurrentDate:GetStoreOrderRequestsWithinTimeRangeOfSeller: StatusOfStoreOrderRequest:AcceptOrderRequest:DeniedOrderRequests]
    (implicit ae: ApplicativeError[F, Throwable], pp: ProcessAndJsonResponse, en: Encoder[Produce], produceDecoder: Decoder[Produce], currencyEncoder: Encoder[Currency], currencyDecoder: Decoder[Currency], zoneDateparser: ParseZoneDateTime, orderStatusEncoder: Encoder[StoreOrderRequestStatus], orderStatusDecoder: Decoder[StoreOrderRequestStatus], ec: ExecutionContext): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of {
      case req @ POST -> Root / "storeOrderRequest" / sellerEmail => {
        parseREquestAndValidateUserAndParseBodyResponse[StoreOrderRequestBody, F](req, {(email, products) =>
          ProcessData(
            implicitly[CreateStoreOrderRequest[F]].createOrder(Email(sellerEmail), email, products.body),
            (l: StoreOrderRequestWithId) => l,
            (err: Throwable) => ResponseBody(s"Error creating store order request: ${err.getMessage()}", false)
          ).process
        })
      }
      case req @ GET -> Root / "storeOrderRequest" / "seller" / from / to => {
        (zoneDateparser.parseZoneDateTime(from), zoneDateparser.parseZoneDateTime(to)) match {
          case (Left(a), _) => Ok(ResponseBody("From date is not valid zone date format", false).asJson.toString())
          case (_, Left(a)) => Ok(ResponseBody("To date is not valid zone date format", false).asJson.toString())
          case (Right(fromDateZone), Right(toDateZone)) => {
            parseRequestAndValidateUserResponse[F](req, {email =>
              ProcessData(
                implicitly[GetStoreOrderRequestsWithinTimeRangeOfSeller[F]].getStoreOrdersWithin(fromDateZone, toDateZone, email),
                (l: List[StoreOrderRequestWithId]) => StoreOrderRequestsBelongingToSellerBody(l),
                (err: Throwable) => ResponseBody(s"Error getting list of store order requests: ${err.getMessage()}", false)
              ).process
            })
          }
        }      
      }
      case GET -> Root / "storeOrderRequest" / "status" / orderId => {
        orderId.toIntOption match {
          case None => Ok(ResponseBody("Order Id is not a integer", false).asJson.toString())
          case Some(id) => {            
            ProcessData(
                implicitly[StatusOfStoreOrderRequest[F]].get(id),
                (s => StoreOrderRequestStatusBody(s)),
                (err: Throwable) => ResponseBody(s"Error getting status of request ${err.getMessage()}", false)
            )
              .process
              .flatMap(a => Ok(a.toString()))
          }
        }
      }
      case req @ POST -> Root / "storeOrderRequest" / "accept" / orderId => {
        orderId.toIntOption match {
          case None => Ok(ResponseBody("Order Id is not a integer", false).asJson.toString())
          case Some(id) => {
            parseRequestAndValidateUserResponse[F](req, {email =>
              ProcessData(
                implicitly[AcceptOrderRequest[F]].accept(id, email),
                (_:Unit) => ResponseBody("Store order request was accepted", true),
                (err: Throwable) => ResponseBody(s"Store order request failed to be accepted, error: ${err.getMessage()}", false)
              ).process
            })
          }
        }
      }
      case req @ POST -> Root / "storeOrderRequest" / "deny" / orderId => {
        orderId.toIntOption match {
          case None => Ok(ResponseBody("Order Id is not a integer", false).asJson.toString())
          case Some(id) => {
            parseRequestAndValidateUserResponse[F](req, {email =>
              ProcessData(
                implicitly[DeniedOrderRequests[F]].deny(id, email),
                (_:Unit) => ResponseBody("Store order request was denied", true),
                (err: Throwable) => ResponseBody(s"Store order request failed to be denied, error: ${err.getMessage()}", false)
              ).process
            })
          }
        }
      }
    }
  }
}
