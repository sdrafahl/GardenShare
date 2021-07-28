package com.gardenShare.gardenshare

import io.circe._
import cats.Show

sealed abstract class StoreOrderRequestStatus

object StoreOrderRequestStatus {
  case object AcceptedRequest extends StoreOrderRequestStatus
  case object DeniedRequest extends StoreOrderRequestStatus
  case object ExpiredRequest extends StoreOrderRequestStatus
  case object RequestToBeDetermined extends StoreOrderRequestStatus
  case object RequestPaidFor extends StoreOrderRequestStatus
  case object SellerComplete extends StoreOrderRequestStatus

  def unapply(s: String): Option[StoreOrderRequestStatus] = parse(s).toOption

  private lazy val storeOrderRequestStatusDecoder: Decoder[StoreOrderRequestStatus] = Decoder.decodeString.emap(parse)
  private lazy val storeOrderRequestStatusEncoder: Encoder[StoreOrderRequestStatus] = Encoder.encodeString.contramap[StoreOrderRequestStatus](encode)
  implicit lazy val storeOrderRequestCodec: Codec[StoreOrderRequestStatus] = Codec.from(storeOrderRequestStatusDecoder, storeOrderRequestStatusEncoder)

  private[this] def parse(x:String): Either[String, StoreOrderRequestStatus] = x match {
    case "AcceptedRequest" => Right(AcceptedRequest)
    case "DeniedRequest" => Right(DeniedRequest)
    case "ExpiredRequest" => Right(ExpiredRequest)
    case "RequestToBeDetermined" => Right(RequestToBeDetermined)
    case "RequestPaidFor" => Right(RequestPaidFor)
    case "SellerComplete" => Right(SellerComplete)
    case _ => Left("Invalid string for order status")
  }

  private[this] def encode(x:StoreOrderRequestStatus): String = x match {
    case AcceptedRequest => "AcceptedRequest"
    case DeniedRequest => "DeniedRequest"
    case ExpiredRequest => "ExpiredRequest"
    case RequestToBeDetermined => "RequestToBeDetermined"
    case RequestPaidFor => "RequestPaidFor"
    case SellerComplete => "SellerComplete"
  }

  implicit object StoreOrderRequestStatusShow extends Show[StoreOrderRequestStatus] {
    override def show(s: StoreOrderRequestStatus) = encode(s)
  }

}

