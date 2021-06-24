package com.gardenShare.gardenshare

import io.circe.Encoder
import io.circe.Decoder
import io.circe.Json
import io.circe.syntax._

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
