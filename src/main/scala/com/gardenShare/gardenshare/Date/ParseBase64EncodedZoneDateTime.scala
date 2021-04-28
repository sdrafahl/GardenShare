package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import scala.util.Try

abstract class ParseBase64EncodedZoneDateTime {
  def parseZoneDateTime(x: String)(implicit b: Base64EncoderDecoder): Either[Throwable, ZonedDateTime]
}

object ParseBase64EncodedZoneDateTime {
  implicit object Default extends ParseBase64EncodedZoneDateTime {
    def parseZoneDateTime(x: String)(implicit b: Base64EncoderDecoder): Either[Throwable, ZonedDateTime] = b.decode(x).flatMap(a => Try(ZonedDateTime.parse(a))).toEither
  }
}
