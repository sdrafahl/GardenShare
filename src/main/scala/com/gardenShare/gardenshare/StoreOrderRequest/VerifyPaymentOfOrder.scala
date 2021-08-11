package com.gardenShare.gardenshare

import cats.effect.ContextShift
import cats.effect.IO
import PaymentCommandEvaluator.PaymentCommandEvaluatorOps
import PaymentID._
import PaymentVerificationStatus.PaymentComplete
import PaymentVerificationStatus.PaymentProcessing
import PaymentVerificationStatus.Canceled
import PaymentVerificationStatus.RequiresFurtherAction
import scala.util._

abstract class VerifyPaymentOfOrder[F[_]] {
  def verifyOrder(orderId: OrderId, buyerEmail: Email)(
      implicit cs: ContextShift[F]
  ): F[PaymentVerification]
}

object VerifyPaymentOfOrder {
  implicit def createIOVerifyPaymentOfOrder(
      implicit evaluator: PaymentCommandEvaluator[IO],
      searchForOrder: SearchStoreOrderRequestTable[IO],
      getPaymentIntent: GetPaymentIntentFromStoreRequest[IO],
      setPayment: SetOrderIsPaid[IO]
  ) = new VerifyPaymentOfOrder[IO] {
    def verifyOrder(orderId: OrderId, buyerEmail: Email)(
        implicit cs: ContextShift[IO]
    ): IO[PaymentVerification] = {
      searchForOrder.search(orderId).flatMap {
        case None => IO.raiseError(new Throwable("Order does not exist"))
        case Some(order) => {
          if (order.storeOrderRequest.buyer.equals(buyerEmail)) {
            getPaymentIntent.search(order.id.toString()).map(maybeIntId => maybeIntId.map(intId => intId.parsePublicKey)).flatMap {
              case None =>
                IO.raiseError(
                    new Throwable(
                    s"No reference found for intent for order id:${order.id}"
                  )
                )
              case Some(Failure(_)) => {
                IO.raiseError(
                    new Throwable(
                    "Error parsing public key"
                  )
                )
              }
              case Some(Success(intentId)) => {                
                for {
                  intent <- GetPaymentIntentCommand(intentId).evaluate
                  paymentStatus = intent.getStatus()
                  stat <- PaymentVerificationStatus.unapply(paymentStatus) match {
                    case Some(PaymentComplete) =>
                      {                        
                        setPayment.setOrder(orderId).map(_ => PaymentComplete)
                      }
                    case Some(Canceled)          => IO.pure(Canceled)
                    case Some(PaymentProcessing) => IO.pure(PaymentProcessing)
                    case Some(RequiresFurtherAction(msg)) =>
                      IO.pure(RequiresFurtherAction(msg))
                    case None =>
                      IO.raiseError(
                        new Throwable(
                          s"There was an error parsing payment verification status."
                        )
                      )
                  }
                } yield stat
              }
            }
          } else {
            IO.raiseError(
              new Throwable("Cannot verify a order for another user")
            )
          }
        }
      }.map(b => PaymentVerification(b))
    }
  }
}
