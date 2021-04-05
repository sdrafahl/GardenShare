package com.gardenShare.gardenshare

import java.time.ZonedDateTime
import scala.util.Try

object ZoneDateTimeParsingEncoding {
  implicit object ZoneDateTimeParser extends Parser[ZonedDateTime] {
    def parse(x:String): Either[String, ZonedDateTime] = Try(ZonedDateTime.parse(x)).toEither.left.map(err => s"There was an error parsing a ZoneDateTime exception message: ${err.getMessage()}")
  }
}
