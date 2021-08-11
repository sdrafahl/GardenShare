package com.gardenShare.gardenshare

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.api.RefType
import io.circe.generic.extras.semiauto._
import io.circe._

import Email._
import io.circe.Codec

sealed case class Email(underlying: EmailValue)

object Email {
  type EmailValue = String Refined MatchesRegex[W.`"""[a-z0-9]+@[a-z0-9]+\\.[a-z0-9]{2,}"""`.T]

  def unapply(str: String): Option[Email] = {
    RefType.applyRef[EmailValue](str).map(x => Email(x)).toOption
  }

  private[this] lazy val emailValueDecoder: Decoder[EmailValue] = Decoder.decodeString.emap(s => RefType.applyRef[EmailValue](s))

  private[this] lazy val emailValueEncoder: Encoder[EmailValue] = Encoder.encodeString.contramap(_.value)

  implicit lazy val emailValueCodec: Codec[EmailValue] = Codec.from(emailValueDecoder, emailValueEncoder)

  implicit lazy val emailCodec: Codec[Email] = deriveUnwrappedCodec
}
