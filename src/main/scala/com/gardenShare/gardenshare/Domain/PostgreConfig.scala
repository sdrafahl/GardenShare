package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class PostgreConfig(url: String, driver: String, connectionPool: String, keepAliveConnection: Boolean)

object PostgreConfig {
  implicit lazy final val postgreConfigCodec: Codec[PostgreConfig] = deriveCodec
}
