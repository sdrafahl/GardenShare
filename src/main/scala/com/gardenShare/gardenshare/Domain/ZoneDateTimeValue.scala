package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import scala.util.Try
import io.circe._
import cats.Show

case class ZoneDateTimeValue(zoneDateTime: ZonedDateTime) extends AnyVal

object ZoneDateTimeValue {

  def unapply(zdtv: String): Option[ZoneDateTimeValue] = parse(zdtv).toOption

  private lazy val currencyDecoder: Decoder[ZoneDateTimeValue] = Decoder.decodeString.emap(parse)

  private lazy val currencyEncoder: Encoder[ZoneDateTimeValue] = Encoder.encodeString.contramap[ZoneDateTimeValue](encode)

  implicit lazy val currencyCodec: Codec[ZoneDateTimeValue] = Codec.from(currencyDecoder, currencyEncoder)

  private[this] def parse(string: String): Either[String, ZoneDateTimeValue] =
    Try(ZonedDateTime.parse(string))
      .toEither
      .left
      .map(err => s"There was an error parsing a ZoneDateTime exception message: ${err.getMessage()}")
      .map(ZoneDateTimeValue(_))

  private[this] def encode(zdtv: ZoneDateTimeValue) = zdtv.zoneDateTime.toString()

  implicit object ZoneDateTimeValueShow extends Show[ZoneDateTimeValue] {
    override def show(x: ZoneDateTimeValue) = encode(x)
  }
}
