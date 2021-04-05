package com.gardenShare.gardenshare

import cats.effect.ContextShift
import cats.implicits._
import cats.effect.IO
import PaymentCommandEvaluator.PaymentCommandEvaluatorOps
import com.gardenShare.gardenshare.Parser
import io.circe.Encoder
import io.circe.Json
import io.circe.Decoder
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.generic.JsonCodec, io.circe.syntax._
import PaymentID._

abstract class VerifyPaymentOfOrder[F[_]] {
  def verifyOrder(orderId: Int, buyerEmail: Email)(
      implicit cs: ContextShift[F]
  ): F[PaymentVerification]
}

object VerifyPaymentOfOrder {
  implicit def createIOVerifyPaymentOfOrder(
      implicit evaluator: PaymentCommandEvaluator[IO],
      searchForOrder: SearchStoreOrderRequestTable[IO],
      getPaymentIntent: GetPaymentIntentFromStoreRequest[IO],
      paymentVerificationStatus: com.gardenShare.gardenshare.Parser[PaymentVerificationStatus],
      setPayment: SetOrderIsPaid[IO]
  ) = new VerifyPaymentOfOrder[IO] {
    def verifyOrder(orderId: Int, buyerEmail: Email)(
        implicit cs: ContextShift[IO]
    ): IO[PaymentVerification] = {
      searchForOrder.search(orderId).flatMap {
        case None => IO.raiseError(new Throwable("Order does not exist"))
        case Some(order) => {
          if (order.storeOrderRequest.buyer.equals(buyerEmail)) {
            getPaymentIntent.search(order.id.toString()).flatMap {
              case None =>
                IO.raiseError(
                    new Throwable(
                    s"No reference found for intent for order id:${order.id}"
                  )
                )
              case Some(intentId) => {                
                for {
                  intent <- GetPaymentIntentCommand(intentId.parsePublicKey).evaluate
                  paymentStatus = intent.getStatus()
                  stat <- paymentVerificationStatus.parse(paymentStatus) match {
                    case Right(PaymentComplete) =>
                      {                        
                        setPayment.setOrder(orderId).map(_ => PaymentComplete)
                      }
                    case Right(Canceled)          => IO.pure(Canceled)
                    case Right(PaymentProcessing) => IO.pure(PaymentProcessing)
                    case Right(RequiresFurtherAction(msg)) =>
                      IO.pure(RequiresFurtherAction(msg))
                    case Left(err) =>
                      IO.raiseError(
                        new Throwable(
                          s"There was an error getting the status ${err}"
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
