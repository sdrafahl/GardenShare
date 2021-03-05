package com.gardenShare.gardenshare

import io.circe.Encoder, io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.circe.KeyEncoder
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.generic.JsonCodec, io.circe.syntax._

object EncodersDecoders {
  implicit val storeOrderRequestStatusEncoder: Encoder[StoreOrderRequestStatus] = Encoder.instance {
    case AcceptedRequest => "AcceptedRequest".asJson
    case DeniedRequest => "DeniedRequest".asJson
    case ExpiredRequest => "ExpiredRequest".asJson
    case RequestToBeDetermined => "RequestToBeDetermined".asJson
  }

  implicit val storeOrderRequestStatusDecoder: Decoder[StoreOrderRequestStatus] = Decoder.decodeString.emap{
    case "AcceptedRequest" => Right(AcceptedRequest)
    case "DeniedRequest" => Right(DeniedRequest)
    case "ExpiredRequest" => Right(ExpiredRequest)
    case "RequestToBeDetermined" => Right(RequestToBeDetermined)
    case _ => Left("Invalid string for order status")
  }
}
