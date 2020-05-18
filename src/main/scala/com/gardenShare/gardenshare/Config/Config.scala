package com.gardenShare.gardenshare.Config

import com.typesafe.config.ConfigFactory


case class MongoSettings(
  host: String,
  port: Int,
  password: String,
  username: String
)

abstract class Config {
  def getMongoSettings(implicit driver: ConfigDriver): MongoSettings
}

object Config {
  implicit def apply() = DefaultConfig
}

object DefaultConfig extends Config {
  val mongoHost = "MongoHost"
  def getMongoSettings(implicit driver: ConfigDriver): MongoSettings = MongoSettings(
    host = driver.getValueString(mongoHost),
    port = driver.getValueInt("MongoPort"),
    password = driver.getValueString("MongoPassword"),
    username = driver.getValueString("MongoUsername")
  )
}

abstract class ConfigDriver {
  def getValueString(key: String): String
  def getValueInt(key: String): Int
}

object ConfigDriver {
  def apply = DefaultConfigDriver  
  implicit object DefaultConfigDriver extends ConfigDriver {
    val conf = ConfigFactory.load()

    val missingConfigErrorMessage = List(
      ("MongoHost", "MongoHost missing from local config, please add the MongoHost value and try restarting the app"),
      ("MongoPort", "MongoPort missing from local config, please add the MongoPort value and try restarting the app"),
      ("MongoPassword", "MongoPassword missing from local config, please add the MongoPassword value and try restarting the app"),
      ("MongoUsername", "MongoUsername missing from local config, please add the MongoUsername value and try restarting the app")
    ).map(keyAndError => (conf.hasPath(keyAndError._1), keyAndError._2))
      .filter(hasKeyAndMessage => ! hasKeyAndMessage._1)
      .toOption()
      .map{
        errorMessages =>
        val errorMessage = errorMessages.foldLeft(""){
          (newErrorMessage, missingConfig) => newErrorMessage + "/n" + missingConfig._2
        }
        throw new Exception(errorMessage)
      }
      
    def getValueString(key: String) = conf.getString(key)
    def getValueInt(key: String) = conf.getInt(key)
  }

  implicit class ListToOption[A](list: List[A]) {
    def toOption(): Option[List[A]] = list match {
      case List() => None
      case _ => Some(list)
    }
  }

}
