package com.gardenShare.gardenshare

import io.circe.{ Decoder, Encoder }
import java.net.URL
import io.circe.syntax._
import scala.util.Try

object ApplyUserToBecomeUserEncodersDecoders {
  implicit val urlEncoder: Encoder[URL] = Encoder.instance {
    case url => url.toExternalForm().asJson
  }

  implicit val urlDecoder: Decoder[URL] = Decoder.decodeString.emap{
    case url => Try(new URL(url))
        .toEither
        .left
        .map(_.getMessage())
  }
}
