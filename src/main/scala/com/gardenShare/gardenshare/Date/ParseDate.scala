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
  implicit object IOGetCurrentDate extends GetCurrentDate[IO] {
    def get:IO[ZonedDateTime] = IO(Instant.now().atZone(ZoneId.of("America/Chicago")))
  }
}

abstract class ParseZoneDateTime {
  def parseZoneDateTime(x: String): Either[Throwable, ZonedDateTime]
}

object ParseZoneDateTime {
  implicit object Default extends ParseZoneDateTime {
    def parseZoneDateTime(x: String): Either[Throwable, ZonedDateTime] = Try(ZonedDateTime.parse(x)).toEither
  }
}
