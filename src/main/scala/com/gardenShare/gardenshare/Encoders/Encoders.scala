package com.gardenShare.gardenshare.Encoders

import io.circe.Encoder, io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import com.gardenShare.gardenshare.UserEntities.Group
import com.gardenShare.gardenshare.UserEntities.Seller
import io.circe.KeyEncoder
import com.gardenShare.gardenshare.Storage.Relational.Order
import com.gardenShare.gardenshare.Storage.Relational.CreateOrderInDB._
import com.gardenShare.gardenshare.Storage.Relational.OrderState

object Encoders {
  implicit val encoder:Encoder[Group] = new Encoder[Group] {
    override def apply(group: Group) = group match {
      case Seller() => Json.fromString("Seller")
      case _ => Json.fromString("unknown")
    }
  }

  implicit val orderEncoder: Encoder[OrderState] = new Encoder[OrderState] {
    final def apply(a: OrderState): Json = Json.fromString(orderToString(a))
  }
}
