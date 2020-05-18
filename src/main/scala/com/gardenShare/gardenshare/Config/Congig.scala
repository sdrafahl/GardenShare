package com.gardenShare.gardenshare.Config

import com.typesafe.config.ConfigFactory


case class MongoSettings(host: String)

abstract class Config {
  def getMongoSettings(implicit driver: ConfigDriver): MongoSettings
}

object Config {
  implicit def apply() = DefaultConfig
}

object DefaultConfig extends Config {
  val mongoHost = "MongoHost"
  def getMongoSettings(implicit driver: ConfigDriver): MongoSettings = MongoSettings(
    host = driver.getValueString(mongoHost)
  )
}

abstract class ConfigDriver {
  def getValueString(key: String): String
}

object ConfigDriver {
  def apply = DefaultConfigDriver
  implicit object DefaultConfigDriver extends ConfigDriver {
    val conf = ConfigFactory.load()
    def getValueString(key: String) = conf.getString(key)
  }
}
