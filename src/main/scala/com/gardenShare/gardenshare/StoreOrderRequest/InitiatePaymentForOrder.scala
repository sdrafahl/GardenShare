package com.gardenShare.gardenshare

import cats.effect.IO
import cats.effect.ContextShift
import cats.implicits._
import StoreOrderRequestStatus._

abstract class InitiatePaymentForOrder[F[_]] {
  def payOrder(orderId: Int, buyerEmail: Email, receiptEmail: Email, paymentType: PaymentType)(implicit cs: ContextShift[F]): F[PaymentIntentToken]
}

object InitiatePaymentForOrder {

  def apply[F[_]: InitiatePaymentForOrder]() = implicitly[InitiatePaymentForOrder[F]]

  implicit def createIOPayForOrder(
    implicit searchForOrder: SearchStoreOrderRequestTable[IO],
    getStatusOfOrder: StatusOfStoreOrderRequest[IO],
    initiatePayment: InitiatePayment[IO],
    insertRef: InsertPaymentIntentReference[IO]
  ) = new InitiatePaymentForOrder[IO] {
    def payOrder(orderId: Int, buyerEmail: Email, receiptEmail: Email, paymentType: PaymentType)(implicit cs: ContextShift[IO]): IO[PaymentIntentToken] = {
      (searchForOrder.search(orderId), getStatusOfOrder.get(orderId))
        .parBisequence
        .flatMap{
          case (None, _) => IO.raiseError(new Throwable(s"No order with orderId ${orderId}"))
          case (_, DeniedRequest) => IO.raiseError(new Throwable(s"Order was denied and cannot be paid for orderId: ${orderId}"))
          case (_, ExpiredRequest) => IO.raiseError(new Throwable(s"Order was expired and cannot be paid for orderId: ${orderId}"))
          case (_, RequestToBeDetermined) => IO.raiseError(new Throwable(s"Order is not accepted so it cannot be paid for orderId: ${orderId}"))
          case (_, RequestPaidFor) => IO.raiseError(new Throwable("Order is already paid for"))
          case (_, SellerComplete) => IO.raiseError(new Throwable("Seller has already completed this"))
          case (Some(order), AcceptedRequest) => {
            if(order.storeOrderRequest.buyer.equals(buyerEmail)) {
              val amountToCharge = order.storeOrderRequest.products.map(_.product.product.am).combineAll
              for {
                intent <- initiatePayment.initiatePayment(amountToCharge, receiptEmail, order.storeOrderRequest.seller, paymentType)
                _ <- insertRef.insert(order.id.toString(), intent.token)
              } yield intent              
            } else {
              IO.raiseError(new Throwable(s"You cannot purchase an order that does not belong to you"))
            }
          }
        }            
    }
  }
}
