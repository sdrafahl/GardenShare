package com.gardenShare.gardenshare.Encoders

import io.circe.Encoder, io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import com.gardenShare.gardenshare.UserEntities.Group
import com.gardenShare.gardenshare.UserEntities.Seller
import io.circe.KeyEncoder

object Encoders {
  implicit val encoder:Encoder[Group] = new Encoder[Group] {
    override def apply(group: Group) = group match {
      case Seller() => Json.fromString("Seller")
      case _ => Json.fromString("unknown")
    }
  }
}
