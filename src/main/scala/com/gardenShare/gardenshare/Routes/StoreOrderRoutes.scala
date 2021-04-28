package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import cats.implicits._
import cats.effect.Async
import com.gardenShare.gardenshare.Helpers._

import io.circe._
import io.circe.generic.auto._, io.circe.syntax._

import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.CreateStoreOrderRequest
import cats.effect.ContextShift
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.Email
import cats.ApplicativeError
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import scala.concurrent.ExecutionContext
import ParsingDecodingImplicits._

object StoreOrderRoutes {
  def storeOrderRoutes[
    F[_]:
        Async:
        ContextShift:
        CreateStoreOrderRequest:
        AuthUser:
        AuthJWT:
        GetCurrentDate:
        GetStoreOrderRequestsWithinTimeRangeOfSeller:
        StatusOfStoreOrderRequest:
        AcceptOrderRequest:
        DeniedOrderRequests:
        JoseProcessJwt:
        InitiatePaymentForOrder:
        VerifyPaymentOfOrder
  ]
    (
      implicit ae: ApplicativeError[F, Throwable],
      zoneDateparser: ParseBase64EncodedZoneDateTime,
      ec: ExecutionContext,
      emailParser: com.gardenShare.gardenshare.Parser[Email],
      en: Encoder[Produce],
      produceDecoder: Decoder[Produce],
      currencyEncoder: Encoder[Currency],
      currencyDecoder: Decoder[Currency],
      orderStatusEncoder: Encoder[StoreOrderRequestStatus],
      orderStatusDecoder: Decoder[StoreOrderRequestStatus]
    ): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of {
      case req @ POST -> Root / "storeOrderRequest" / sellerEmail => {
        emailParser.parse(sellerEmail) match {
          case Left(_) => Ok(ResponseBody("seller email provide is not a valid email address", false).asJson.toString())
          case Right(emailOfSeller) => {
            parseREquestAndValidateUserAndParseBodyResponse[StoreOrderRequestBody, F](req, {(email, products) =>
              ProcessData(
                implicitly[CreateStoreOrderRequest[F]].createOrder(emailOfSeller, email, products.body),
                (l: StoreOrderRequestWithId) => l,
                (err: Throwable) => ResponseBody(s"Error creating store order request: ${err.getMessage()}", false)
              ).process
            })
          }
        }               
      }
      case req @ GET -> Root / "storeOrderRequest" / "seller" / from / to => {
        (zoneDateparser.parseZoneDateTime(from), zoneDateparser.parseZoneDateTime(to)) match {
          case (Left(_), _) => Ok(ResponseBody("From date is not valid zone date format", false).asJson.toString())
          case (_, Left(_)) => Ok(ResponseBody("To date is not valid zone date format", false).asJson.toString())
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
      case req @ POST -> Root / "storeOrderRequest" / "initiate-payment" / orderId / receiptEmail / paymentType => {
        (orderId.toIntOption, implicitly[com.gardenShare.gardenshare.Parser[Email]].parse(receiptEmail), implicitly[com.gardenShare.gardenshare.Parser[PaymentType]].parse(paymentType)) match {
          case (None, _, _) => Ok(ResponseBody("Order Id is not a valid id", false).asJson.toString())
          case (_, Left(_), _) => Ok(ResponseBody("Receipt email is not a valid email", false).asJson.toString())
          case (_, _, Left(_)) => Ok(ResponseBody("Payment type is not valid", false).asJson.toString())
          case (Some(id), Right(emailToSendReceipt), Right(pymtType)) => {
            parseRequestAndValidateUserResponse[F](req, {buyerEmail =>
              ProcessData(
                implicitly[InitiatePaymentForOrder[F]].payOrder(id, buyerEmail, emailToSendReceipt, pymtType),
                (tkn: PaymentIntentToken) => tkn,
                (err: Throwable) => ResponseBody(s"There was a problem initiating a payment message: ${err.getMessage()}", false)
              ).process
            })
          }
        }
      }
      case req @ POST -> Root / "storeOrderRequest" / "verify-payment" / orderId => {
        orderId.toIntOption match {
          case None => Ok(ResponseBody(s"Order ID: ${orderId} is not a valid order ID", false).asJson.toString())
          case Some(order_id) => {
            parseRequestAndValidateUserResponse[F](req, {buyerEmail =>
              ProcessData(
                implicitly[VerifyPaymentOfOrder[F]].verifyOrder(order_id, buyerEmail),
                (x: PaymentVerification) => x,
                (err: Throwable) => ResponseBody(s"Error in verification error: ${err.getMessage()}", false)
              ).process
            })
          }
        }
      }
    }
  }
}
