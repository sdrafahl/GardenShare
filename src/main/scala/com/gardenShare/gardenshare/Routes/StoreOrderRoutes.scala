package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import cats.implicits._
import cats.effect.Async
import io.circe.generic.auto._
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.CreateStoreOrderRequest
import cats.effect.ContextShift
import com.gardenShare.gardenshare.Email
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.SellerCompleteOrder._
import AuthenticateJWTOnRequest.AuthenticateJWTOnRequestOps
import ProcessPolymorphicType.ProcessPolymorphicTypeOps
import org.http4s.circe.CirceEntityCodec._

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
        SellerCompleteOrder:
        ProcessPolymorphicType
  ]
    (
      implicit ec: ExecutionContext
    ): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of {
      case req @ POST -> Root / "storeOrderRequest" / Email(sellerEmail) => {
        for {
          email <- req.authJWT
          products <- req.as[StoreOrderRequestBody]
          response <- implicitly[CreateStoreOrderRequest[F]].createOrder(sellerEmail, email, products.body).asJsonF
        } yield response        
      }
      case req @ GET -> Root / "storeOrderRequest" / "seller" / ZoneDateTimeValue(from) / ZoneDateTimeValue(to) => {
        for {
          email <- req.authJWT
          responseToUser <- implicitly[GetStoreOrderRequestsWithinTimeRangeOfSeller[F]]
          .getStoreOrdersWithin(from.zoneDateTime, to.zoneDateTime, email)
          .map(StoreOrderRequestsBelongingToSellerBody(_))
          .asJsonF
        } yield responseToUser
      }
      case GET -> Root / "storeOrderRequest" / "status" / OrderId(orderId) => {
        for {
          statusOfStoreOrder <- implicitly[StatusOfStoreOrderRequest[F]]
          .get(orderId)
          .map(StoreOrderRequestStatusBody(_))
          .asJsonF
        } yield statusOfStoreOrder
      }
      case req @ POST -> Root / "storeOrderRequest" / "accept" / OrderId(id) => {
        (for {
          userEmail <- req.authJWT
          _ <- implicitly[AcceptOrderRequest[F]].accept(id, userEmail)
        } yield ResponseBody("Store order request was accepted", true))
          .asJsonF        
      }
      case req @ POST -> Root / "storeOrderRequest" / "deny" / OrderId(id) => {
        (for {
          userEmail <- req.authJWT
          _ <- implicitly[DeniedOrderRequests[F]].deny(id, userEmail)
        } yield ResponseBody("Store order request was denied", true))
          .asJsonF        
      }
      case req @ POST -> Root / "storeOrderRequest" / "initiate-payment" / OrderId(orderId) / Email(receiptEmail) / PaymentType(paymentType) => {
        for {
          buyerEmail <- req.authJWT
          response <- implicitly[InitiatePaymentForOrder[F]]
          .payOrder(orderId, buyerEmail, receiptEmail, paymentType)
          .asJsonF
        } yield response
      }
      case req @ POST -> Root / "storeOrderRequest" / "verify-payment" / OrderId(orderId) => {
        for {
          buyerEmail <- req.authJWT
          paymentVerification <- implicitly[VerifyPaymentOfOrder[F]].verifyOrder(orderId, buyerEmail).asJsonF
        } yield paymentVerification
      }
      case req @ POST -> Root / "storeOrderRequest" / "seller-complete-order" / OrderId(orderId) => {
        (for {
          sellerEmail <- req.authJWT
          _ <- SellerCompleteOrderRequest(orderId, sellerEmail).complete[F]
        } yield ResponseBody(s"Order: ${orderId} is confirmed to be complete by seller", true))
          .asJsonF        
      }
    }
  }
}
