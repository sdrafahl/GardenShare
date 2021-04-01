package com.gardenShare.gardenshare

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.api.RefType
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.generic.JsonCodec, io.circe.syntax._
import io.circe.Encoder
import io.circe.Decoder
import io.circe.Json

case class Email(underlying: EmailCompanion.EmailValue)

object EmailCompanion {
  type EmailValue = String Refined MatchesRegex[W.`"""[a-z0-9]+@[a-z0-9]+\\.[a-z0-9]{2,}"""`.T]
  implicit object EmailParser extends Parser[Email] {
    def parse(s: String): Either[String, Email] = RefType.applyRef[EmailValue](s).left.map(s => s"Failed to parse Email: ${s}").map(x => Email(x))
  }

  lazy implicit val emailValueEncoder: Encoder[EmailValue] = new Encoder[EmailValue] {
    final def apply(a: EmailValue): Json = a.value.asJson
  }

  implicit def createEmailValueDecoder(implicit parser: Parser[Email]): Decoder[EmailValue] = Decoder.decodeString.emap((s: String) => parser.parse(s).left.map(err => s"There was an error decoding an email for ${s}").map(_.underlying))
}
