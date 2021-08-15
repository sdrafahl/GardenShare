package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class ResponseBody(msg: String, success: Boolean)

object ResponseBody {
  implicit lazy final val responseBodyCodec: Codec[ResponseBody] = deriveCodec
}
