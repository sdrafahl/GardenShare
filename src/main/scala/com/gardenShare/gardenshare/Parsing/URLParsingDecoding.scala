package com.gardenShare.gardenshare

import java.net.URL
import scala.util.Try

object URLParsingDecoding {
  implicit object URLParser extends Parser[URL] {
    def parse(x:String): Either[String, URL] =
      Try(new URL(x))
      .toEither
      .left
      .map(_.getMessage())
  }

  implicit object URLEncoder extends EncodeToString[URL] {
    def encode(x:URL): String = x.toExternalForm()
  }
}
