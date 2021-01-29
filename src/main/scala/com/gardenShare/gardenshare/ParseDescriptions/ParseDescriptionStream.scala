package com.gardenShare.gardenshare.ParseDescription

import com.gardenShare.gardenshare.domain.Products._
import io.circe.generic.auto._, io.circe.syntax._
import io.circe._, io.circe.generic.semiauto._
import io.circe.parser._
import com.gardenShare.gardenshare.domain.S3.LazyStream
import cats.effect.IO
import io.circe.fs2._
import io.circe.generic.auto._

abstract class ParseDescriptionStream[F[_]] {
  def parseStream(a: LazyStream[F, Byte])(implicit d: Decoder[PriceUnit]): LazyStream[F, ProductDescription]
}

object ParseDescriptionStream {
  implicit object IOParseDescriptionStream extends ParseDescriptionStream[IO] {
    def parseStream(a: LazyStream[IO, Byte])(implicit d: Decoder[PriceUnit]): LazyStream[IO, ProductDescription] = {
      LazyStream(a
        .s
        .map(_.through(byteStreamParser))
        .map(_.through(decoder[IO, ProductDescription])))
    }
  }
  implicit class ParseDescriptionStreamOp[F[_]: ParseDescriptionStream](underlying: LazyStream[F, Byte]) {
    def parse(implicit parser: ParseDescriptionStream[F], d: Decoder[PriceUnit]) = parser.parseStream(underlying)
  }
}
