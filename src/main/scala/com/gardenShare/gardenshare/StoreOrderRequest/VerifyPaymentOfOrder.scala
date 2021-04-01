package com.gardenShare.gardenshare

import cats.effect.ContextShift
import cats.implicits._
import AmountOps._
import cats.effect.IO
import PaymentCommandEvaluator.PaymentCommandEvaluatorOps
import com.gardenShare.gardenshare.Parser
import io.circe.Encoder
import io.circe.Json
import io.circe.Decoder
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.generic.JsonCodec, io.circe.syntax._
import PaymentID._


sealed abstract class PaymentVerificationStatus
case object PaymentComplete extends PaymentVerificationStatus
case object PaymentProcessing extends PaymentVerificationStatus
case object Canceled extends PaymentVerificationStatus
case class RequiresFurtherAction(msg: String) extends PaymentVerificationStatus

object PaymentVerificationStatus {
  implicit object PaymentVerificationStatusParser
      extends Parser[PaymentVerificationStatus] {
    def parse(x: String): Either[String, PaymentVerificationStatus] = {
      x match {
        case "requires_payment_method" =>
          Right(RequiresFurtherAction("requires_payment_method"))
        case "requires_confirmation" =>
          Right(RequiresFurtherAction("requires_confirmation"))
        case "requires_action" =>
          Right(RequiresFurtherAction("requires_action"))
        case "processing" => Right(PaymentProcessing)
        case "requires_capture" =>
          Right(RequiresFurtherAction("requires_capture"))
        case "canceled"  => Right(Canceled)
        case "succeeded" => Right(PaymentComplete)
        case _           => Left("Invalid paymentstatus")
      }
    }
  }

  implicit object PaymentVerificationStatusDecoder extends EncodeToString[PaymentVerificationStatus] {
    def encode(x:PaymentVerificationStatus): String = x match {
      case RequiresFurtherAction(msg) => msg
      case PaymentProcessing => "processing"
      case Canceled => "canceled"
      case PaymentComplete => "succeeded"
    }
  }

  implicit def createPaymentVerificationStatusEncoder(implicit parser: Parser[PaymentVerificationStatus]) = Decoder.decodeString.emap{(s: String) => parser.parse(s)}

  implicit def createPaymentVerificationDecoder(implicit encodeToString: EncodeToString[PaymentVerificationStatus]) = new Encoder[PaymentVerificationStatus] {
    final def apply(a: PaymentVerificationStatus): Json = encodeToString.encode(a).asJson
  }
}

case class PaymentVerification(status: PaymentVerificationStatus)

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
      paymentVerificationStatus: com.gardenShare.gardenshare.Parser[
        PaymentVerificationStatus
      ],
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
