package com.gardenShare.gardenshare

import io.circe.{ Decoder, Encoder }
import java.net.URL
import io.circe.syntax._
import scala.util.Try
import io.circe.Codec

object EncodersDecoders {
  private lazy val urlEncoder: Encoder[URL] = Encoder.instance {
    case url => url.toExternalForm().asJson
  }

  private lazy val urlDecoder: Decoder[URL] = Decoder.decodeString.emap{
    case url => Try(new URL(url))
        .toEither
        .left
        .map(_.getMessage())
  }

  implicit lazy val urlCodec: Codec[URL] = Codec.from(urlDecoder, urlEncoder)
}
