package com.gardenShare.gardenshare

import io.circe._

sealed abstract class PaymentVerificationStatus

object PaymentVerificationStatus {

  case object PaymentComplete extends PaymentVerificationStatus
  case object PaymentProcessing extends PaymentVerificationStatus
  case object Canceled extends PaymentVerificationStatus
  case class RequiresFurtherAction(msg: String) extends PaymentVerificationStatus

  def unapply(s: String): Option[PaymentVerificationStatus] = parse(s).toOption

  private[this] def parse(x: String): Either[String, PaymentVerificationStatus] = {
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

  private[this] def encode(x:PaymentVerificationStatus): String = x match {
    case RequiresFurtherAction(msg) => msg
    case PaymentProcessing => "processing"
    case Canceled => "canceled"
    case PaymentComplete => "succeeded"
  }

  private[this] lazy val paymentVerificationStatusDecoder: Decoder[PaymentVerificationStatus] = Decoder.decodeString.emap(parse)

  private[this] lazy val paymentVerificationStatusEncoder: Encoder[PaymentVerificationStatus] = Encoder.encodeString.contramap[PaymentVerificationStatus](encode)

  implicit lazy val paymentVerificationStatusCodec: Codec[PaymentVerificationStatus] = Codec.from(paymentVerificationStatusDecoder, paymentVerificationStatusEncoder)
}
