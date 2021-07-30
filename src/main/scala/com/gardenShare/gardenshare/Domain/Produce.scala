package com.gardenShare.gardenshare

import io.circe._

sealed abstract class Produce

object Produce {

  case object BrownOysterMushrooms extends Produce

  def unapply(str: String): Option[Produce] = str match {
    case "BrownOysterMushrooms" => Option(BrownOysterMushrooms)
    case _ => None
  }

  private[this] lazy val produceDecoder: Decoder[Produce] = Decoder.decodeString.emap(sc => unapply(sc).fold[Either[String, Produce]](Left("Invalid produce"))(v => Right(v)))
  private[this] lazy val produceEncoder: Encoder[Produce] = Encoder.encodeString.contramap[Produce] {
    case BrownOysterMushrooms => "BrownOysterMushrooms"
  }

  implicit lazy val produceCodec: Codec[Produce] = Codec.from(produceDecoder, produceEncoder)
}

