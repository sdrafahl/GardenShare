package com.gardenShare.gardenshare

import io.circe._
import java.net.URL
import scala.util.Try
import cats.Show

case class UrlValue(url: URL) extends AnyVal

object UrlValue {

  def unapply(url: String) = parse(url).toOption

  private lazy val currencyDecoder: Decoder[UrlValue] = Decoder.decodeString.emap(parse)

  private lazy val currencyEncoder: Encoder[UrlValue] = Encoder.encodeString.contramap[UrlValue](encode)

  implicit lazy val UrlValueCodec: Codec[UrlValue] = Codec.from(currencyDecoder, currencyEncoder)

  implicit object UrlValueShow extends Show[UrlValue] {
    override def show(url: UrlValue) = encode(url)
  }

  private[this] def encode(url: UrlValue): String = url.url.toExternalForm()

  private[this] def parse(urlString: String): Either[String, UrlValue] =
    Try(new URL(urlString))
      .toEither
      .left
      .map(_.getMessage())
      .map(UrlValue(_))
}
