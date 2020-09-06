package com.gardenShare.gardenshare.ParseGroups

import com.gardenShare.gardenshare.UserEntities.Group
import com.gardenShare.gardenshare.UserEntities.Seller

abstract class ParseGroups {
  def parse(msg: String): List[Group]
}

object ParseGroups {
  def apply() = default
  implicit object default extends ParseGroups {
    def parse(msg: String): List[Group] = {
      msg.contains("Sellers") match {
        case true => List(Seller())
        case false => List()
      }     
    }
  }
  implicit class ParseOps(underlying:String) {
    def parseGroups(implicit parser: ParseGroups) = parser.parse(underlying)
  }
}
