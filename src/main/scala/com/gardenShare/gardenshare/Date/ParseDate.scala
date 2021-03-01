package com.gardenShare.gardenshare

import java.{util => ju}
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import scala.util.Try
import cats.effect.IO

abstract class GetCurrentDate[F[_]] {
  def get: F[ZonedDateTime]
}

object GetCurrentDate {
  def apply[F[_]: GetCurrentDate]() = implicitly[GetCurrentDate[F]]

  implicit object IOGetCurrentDate extends GetCurrentDate[IO] {
    def get:IO[ZonedDateTime] = IO(Instant.now().atZone(ZoneId.of("America/Chicago")))
  }
}

abstract class ParseDate {
  def parseDate(x: String): Either[Throwable, ZonedDateTime]
}

object ParseDate {  
  implicit object Default extends ParseDate {
    def parseDate(x: String): Either[Throwable, ZonedDateTime] = Try(ZonedDateTime.parse(x)).toEither
  }
}

abstract class ParseZoneDateTime {
  def parseZoneDateTime(x: String)(implicit b: Base64EncoderDecoder): Either[Throwable, ZonedDateTime]
}

object ParseZoneDateTime {
  implicit object Default extends ParseZoneDateTime {
    def parseZoneDateTime(x: String)(implicit b: Base64EncoderDecoder): Either[Throwable, ZonedDateTime] = b.decode(x).flatMap(a => Try(ZonedDateTime.parse(a))).toEither
  }
}

