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

object Encoders {
  implicit val orderEncoder: Encoder[OrderState] = new Encoder[OrderState] {
    final def apply(a: OrderState): Json = Json.fromString(orderToString(a))
  }

  implicit object UserTypeEncoder extends Encoder[UserType] {
    override def apply(ut: UserType) = ut match {
      case Requester => Requester.asJson
      case Sellers => Sellers.asJson
      case InvalidType => InvalidType.asJson
    }
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

}
