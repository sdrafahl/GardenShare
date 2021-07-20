package com.gardenShare.gardenshare

import io.circe._

sealed trait UserType

object UserType {

  case object Requester extends UserType
  case object Sellers extends UserType
  case object InvalidType extends UserType

  private[this] def parse(x: String): Either[String, UserType] = x match {
    case "Requester" => Right(Requester)
    case "Sellers" => Right(Sellers)
    case _ => Left("Invalid UserType")
  }

  private[this] def encode(x: UserType): String = x match {
    case Requester => "Requester"
    case Sellers => "Sellers"
    case InvalidType => "InvalidType"
  }

  private lazy val userTypeDecoder: Decoder[UserType] = Decoder.decodeString.emap(parse)

  private lazy val userTypeEncoder: Encoder[UserType] = Encoder.encodeString.contramap[UserType](encode)

  implicit lazy val userTypeCodec: Codec[UserType] = Codec.from(userTypeDecoder, userTypeEncoder)

}
