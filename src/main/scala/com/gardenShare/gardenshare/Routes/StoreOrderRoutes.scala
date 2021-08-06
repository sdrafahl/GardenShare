package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import cats.implicits._
import cats.effect.Async
import com.gardenShare.gardenshare.Helpers._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.CreateStoreOrderRequest
import cats.effect.ContextShift
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.SellerCompleteOrder._

object StoreOrderRoutes {
  def storeOrderRoutes[
    F[_]:
        Async:
        ContextShift:
        CreateStoreOrderRequest:
        AuthJWT:
        GetCurrentDate:
        GetStoreOrderRequestsWithinTimeRangeOfSeller:
        StatusOfStoreOrderRequest:
        AcceptOrderRequest:
        DeniedOrderRequests:
        InitiatePaymentForOrder:
        VerifyPaymentOfOrder:
        SellerCompleteOrder
  ]
    (
      implicit zoneDateparser: ParseBase64EncodedZoneDateTime,
      ec: ExecutionContext
    ): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of {
      case req @ POST -> Root / "storeOrderRequest" / Email(sellerEmail) => {
        parseREquestAndValidateUserAndParseBodyResponse[StoreOrderRequestBody, F](req, {(email, products) =>
          ProcessData(
            implicitly[CreateStoreOrderRequest[F]].createOrder(sellerEmail, email, products.body),
            (l: StoreOrderRequestWithId) => l,
            (err: Throwable) => ResponseBody(s"Error creating store order request: ${err.getMessage()}", false)
          ).process
        })
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
      case req @ POST -> Root / "storeOrderRequest" / "initiate-payment" / IntVar(orderId) / Email(receiptEmail) / PaymentType(paymentType) => {
        parseRequestAndValidateUserResponse[F](req, {buyerEmail =>
          ProcessData(
            implicitly[InitiatePaymentForOrder[F]].payOrder(orderId, buyerEmail, receiptEmail, paymentType),
            (tkn: PaymentIntentToken) => tkn,
            (err: Throwable) => ResponseBody(s"There was a problem initiating a payment message: ${err.getMessage()}", false)
          ).process
        })
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
      case req @ POST -> Root / "storeOrderRequest" / "seller-complete-order" / orderId => {
        orderId.toIntOption match {
          case None => Ok(ResponseBody(s"Order ID: ${orderId} is not a valid order ID", false).asJson.toString())
          case Some(order_id) => {
            parseRequestAndValidateUserResponse[F](req, {sellerEmail =>
              
              ProcessData(
                SellerCompleteOrderRequest(order_id, sellerEmail).complete[F],
                (_: Unit) => ResponseBody(s"Order: ${orderId} is confirmed to be complete by seller", true),
                (err: Throwable) => ResponseBody(s"Error in confirmation: ${err.getMessage()}", false)
              ).process
            })
          }
        }
      }
    }
  }
}
