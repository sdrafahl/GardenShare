package com.gardenShare.gardenshare.ParseDescription

import com.gardenShare.gardenshare.domain.Products._
import io.circe.generic.auto._, io.circe.syntax._
import io.circe._, io.circe.generic.semiauto._
import io.circe.parser._

abstract class ParseDescription[A] {
  def parseToDescription(a: A): Either[BadDescription, ProductDescription]
}

object ParseDescription {
  implicit def apply[A: ParseDescription]() = implicitly[ParseDescription[A]]
  implicit object StringParser extends ParseDescription[String] {
    def parseToDescription(a: String): Either[BadDescription, ProductDescription] = {
      parse(a).map(_.as[ProductDescription].toOption) match {
        case Right(Some(desc)) => Right(desc)
        case Right(None) => Left(BadDescription("Result was empty"))
        case Left(error) => Left(BadDescription(error.message))
      }
    }
  }
  implicit class Ops[A: ParseDescription](underlying: A) {
    def parseToDesc(implicit parser: ParseDescription[A]) = parser.parseToDescription(underlying)
  }
}
