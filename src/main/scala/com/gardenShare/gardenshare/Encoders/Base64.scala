package com.gardenShare.gardenshare

import java.util.Base64
import java.nio.charset.StandardCharsets
import scala.util.Try

abstract class Base64EncoderDecoder {
  def decode(s: String): Try[String]
  def encode(s: String): Try[String]
}

object Base64EncoderDecoder {
  def apply() = Default

  implicit object Default extends Base64EncoderDecoder {
    def decode(s: String): Try[String] = {
      Try{
        val bytes = Base64.getDecoder().decode(s)
        new String(bytes, StandardCharsets.UTF_8)
      }
    }
    def encode(s: String): Try[String] = Try(Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)))
  }
}
