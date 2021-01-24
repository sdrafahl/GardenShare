package com.gardenShare.gardenshare.Encoders

import io.circe.Encoder, io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.circe.KeyEncoder
import com.gardenShare.gardenshare.Storage.Relational.Order
import com.gardenShare.gardenshare.Storage.Relational.CreateOrderInDB._
import com.gardenShare.gardenshare.Storage.Relational.OrderState
import com.gardenShare.gardenshare.UserEntities._
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.generic.JsonCodec, io.circe.syntax._
import com.gardenShare.gardenshare.domain._
import cats.syntax.functor._
import io.circe.{ Decoder, Encoder }, io.circe.generic.auto._
import io.circe.syntax._
import com.gardenShare.gardenshare.UserEntities.Requester
import cats.syntax.functor._
import io.circe.{ Decoder, Encoder }, io.circe.generic.auto._
import io.circe.syntax._

object Encoders {
  implicit val orderEncoder: Encoder[OrderState] = new Encoder[OrderState] {
    final def apply(a: OrderState): Json = Json.fromString(orderToString(a))
  }

  implicit val UserTypeEncoder: Encoder[UserType] = Encoder.instance {
    case Requester => "Requester".asJson
    case Sellers => "Sellers".asJson
    case InvalidType => "InvalidType".asJson
  }

  implicit object CreateWorkerResponseEncoder extends Encoder[CreateWorkerResponse] {
    override def apply(ut: CreateWorkerResponse) = ut match {
      case WorkerCreatedSuccessfully() => WorkerCreatedSuccessfully().asJson
      case WorkerFailedToCreate(msg) => WorkerFailedToCreate(msg).asJson
    }
  }

  implicit object SellerResponseEncoder extends Encoder[SellerResponse] {
    override def apply(sr: SellerResponse) = sr match {
      case SellerRequestSuccessful() => SellerRequestSuccessful().asJson
      case SellerRequestFailed(msg) => SellerRequestFailed(msg).asJson
    }
  }

  implicit val sellerResponseDecoder = List[Decoder[SellerResponse]](
    Decoder[SellerRequestSuccessful].widen,
    Decoder[SellerRequestFailed].widen
  ).reduceLeft(_ or _)

  implicit val CreateWorkerResponseDecoder = List[Decoder[CreateWorkerResponse]](
    Decoder[WorkerCreatedSuccessfully].widen,
    Decoder[WorkerFailedToCreate].widen
  ).reduceLeft(_ or _)

  // implicit val userTypeDecoder = List[Decoder[UserType]](
  //   Decoder[Requester].widen,
  //   Decoder[Sellers].widen,
  //   Decoder[InvalidType].widen
  // ).reduceLeft(_ or _)

  implicit val userTypeDecoder: Decoder[UserType] = Decoder.decodeString.emap{
    case "Requester" => Right(Requester)
    case "Sellers" => Right(Sellers)
    case _ => Right(InvalidType)
  }
}
