package com.gardenShare.gardenshare

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.api.RefType

import Email._

sealed case class Email(underlying: EmailValue)

object Email {
  type EmailValue = String Refined MatchesRegex[W.`"""[a-z0-9]+@[a-z0-9]+\\.[a-z0-9]{2,}"""`.T]

  def unapply(str: String): Option[Email] = {
    RefType.applyRef[EmailValue](str).map(x => Email(x)).toOption
  }

  implicit object EmailParser extends Parser[Email] {
    def parse(s: String): Either[String, Email] = RefType.applyRef[EmailValue](s).left.map(s => s"Failed to parse Email: ${s}").map(x => Email(x))
  }

  implicit object EmailEncoder extends EncodeToString[Email] {
    def encode(x:Email): String = x.underlying.value
  }
}
