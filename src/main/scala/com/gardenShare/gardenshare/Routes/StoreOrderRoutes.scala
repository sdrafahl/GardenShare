package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import cats.implicits._
import cats.effect.Async
import io.circe.generic.auto._
import com.gardenShare.gardenshare.CreateStoreOrderRequest
import cats.effect.ContextShift
import com.gardenShare.gardenshare.Email
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.SellerCompleteOrder._
import ProcessPolymorphicType.ProcessPolymorphicTypeOps
import org.http4s.circe.CirceEntityCodec._
import org.http4s.AuthedRoutes
import org.http4s.server.AuthMiddleware

object StoreOrderRoutes {
  def storeOrderRoutes[
    F[_]:
        Async:
        ContextShift:
        CreateStoreOrderRequest:
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
      implicit ec: ExecutionContext,
      authMiddleWear: AuthMiddleware[F, Email]
    ): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._

    val authedRoutes = AuthedRoutes.of[Email, F] {
      case req @ POST -> Root / "storeOrderRequest" / Email(sellerEmail) as email => for {
        products <- req.req.as[StoreOrderRequestBody]
        response <- implicitly[CreateStoreOrderRequest[F]].createOrder(sellerEmail, email, products.body).asJsonF
      } yield response

      case GET -> Root / "storeOrderRequest" / "seller" / ZoneDateTimeValue(from) / ZoneDateTimeValue(to) as email => for {
        responseToUser <- implicitly[GetStoreOrderRequestsWithinTimeRangeOfSeller[F]]
          .getStoreOrdersWithin(from.zoneDateTime, to.zoneDateTime, email)
          .map(StoreOrderRequestsBelongingToSellerBody(_))
          .asJsonF
      } yield responseToUser

      case POST -> Root / "storeOrderRequest" / "accept" / OrderId(id) as userEmail =>  (for {
        _ <- implicitly[AcceptOrderRequest[F]].accept(id, userEmail)
      } yield ResponseBody("Store order request was accepted", true))
          .asJsonF

      case POST -> Root / "storeOrderRequest" / "deny" / OrderId(id) as userEmail => (for {
        _ <- implicitly[DeniedOrderRequests[F]].deny(id, userEmail)
      } yield ResponseBody("Store order request was denied", true)).asJsonF

      case POST -> Root / "storeOrderRequest" / "initiate-payment" / OrderId(orderId) / Email(receiptEmail) / PaymentType(paymentType) as buyerEmail => for {
        response <- implicitly[InitiatePaymentForOrder[F]]
        .payOrder(orderId, buyerEmail, receiptEmail, paymentType)
        .asJsonF
      } yield response

      case POST -> Root / "storeOrderRequest" / "verify-payment" / OrderId(orderId) as buyerEmail => for {
        paymentVerification <- implicitly[VerifyPaymentOfOrder[F]].verifyOrder(orderId, buyerEmail).asJsonF
      } yield paymentVerification

      case POST -> Root / "storeOrderRequest" / "seller-complete-order" / OrderId(orderId) as sellerEmail => (for {
        _ <- SellerCompleteOrderRequest(orderId, sellerEmail).complete[F]
      } yield ResponseBody(s"Order: ${orderId} is confirmed to be complete by seller", true))
          .asJsonF
    }

    val unAuthedRoutes = HttpRoutes.of[F] {
      case GET -> Root / "storeOrderRequest" / "status" / OrderId(orderId) => {
        for {
          statusOfStoreOrder <- implicitly[StatusOfStoreOrderRequest[F]]
          .get(orderId)
          .map(StoreOrderRequestStatusBody(_))
          .asJsonF
        } yield statusOfStoreOrder
      }                              
    }

    unAuthedRoutes <+> authMiddleWear(authedRoutes)
  }
}
