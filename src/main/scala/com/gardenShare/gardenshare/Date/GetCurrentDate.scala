package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import java.time.Instant
import cats.effect.IO
import java.time.ZoneId

abstract class GetCurrentDate[F[_]] {
  def get: F[ZonedDateTime]
}

object GetCurrentDate {
  def apply[F[_]: GetCurrentDate]() = implicitly[GetCurrentDate[F]]

  implicit object IOGetCurrentDate extends GetCurrentDate[IO] {
    def get:IO[ZonedDateTime] = IO(Instant.now().atZone(ZoneId.of("America/Chicago")))
  }
}
