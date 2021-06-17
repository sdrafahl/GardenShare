package com.gardenShare.gardenshare

sealed abstract class StoreOrderRequestStatus
case object AcceptedRequest extends StoreOrderRequestStatus
case object DeniedRequest extends StoreOrderRequestStatus
case object ExpiredRequest extends StoreOrderRequestStatus
case object RequestToBeDetermined extends StoreOrderRequestStatus
case object RequestPaidFor extends StoreOrderRequestStatus
case object SellerComplete extends StoreOrderRequestStatus

object StoreOrderRequestStatus {
  implicit object StoreOrderRequestStatusParser extends Parser[StoreOrderRequestStatus] {
    def parse(x:String): Either[String, StoreOrderRequestStatus] = x match {
      case "AcceptedRequest" => Right(AcceptedRequest)
      case "DeniedRequest" => Right(DeniedRequest)
      case "ExpiredRequest" => Right(ExpiredRequest)
      case "RequestToBeDetermined" => Right(RequestToBeDetermined)
      case "RequestPaidFor" => Right(RequestPaidFor)
      case "SellerComplete" => Right(SellerComplete)
      case _ => Left("Invalid string for order status")
    }
  }

  implicit object StoreOrderRequestStatusEncoder extends EncodeToString[StoreOrderRequestStatus] {
    def encode(x:StoreOrderRequestStatus): String = x match {
      case AcceptedRequest => "AcceptedRequest"
      case DeniedRequest => "DeniedRequest"
      case ExpiredRequest => "ExpiredRequest"
      case RequestToBeDetermined => "RequestToBeDetermined"
      case RequestPaidFor => "RequestPaidFor"
      case SellerComplete => "SellerComplete"
    }
  }
}

