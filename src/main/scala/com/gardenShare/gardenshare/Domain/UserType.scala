package com.gardenShare.gardenshare

sealed trait UserType
case object Requester extends UserType
case object Sellers extends UserType
case object InvalidType extends UserType

object UserType {
  implicit object UserTypeParser extends Parser[UserType] {
    def parse(x: String): Either[String, UserType] = x match {
      case "Requester" => Right(Requester)
      case "Sellers" => Right(Sellers)
      case _ => Left("Invalid UserType")
    }
  }

  implicit object UserTypeEncoder extends EncodeToString[UserType] {
    def encode(x: UserType): String = x match {
      case Requester => "Requester"
      case Sellers => "Sellers"
      case InvalidType => "InvalidType"
    }    
  }
}
