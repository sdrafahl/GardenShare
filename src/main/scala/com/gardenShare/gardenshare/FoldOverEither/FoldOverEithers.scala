package com.gardenShare.gardenshare.FoldOver

import io.circe._, io.circe.parser._
import io.circe.syntax._
import cats.Applicative
import cats.syntax.applicative._

abstract class FoldOverEithers {
  def foldOver[B: Encoder, F[_]: Applicative](a: Either[B, F[Json]]): F[Json]
}

object FoldOverEithers {
  implicit object DefaultFoldOverIntoJson extends FoldOverEithers {
    def foldOver[B: Encoder, F[_]: Applicative](a: Either[B, F[Json]]): F[Json] = a.left.map(v => Applicative[F].pure((v.asJson))).fold(a => a, b => b)
  }

  implicit class FoldOverIntoJsonOps[B: Encoder, F[_]: Applicative](underlying: Either[B, F[Json]]) {
    def foldIntoJson(implicit folder: FoldOverEithers) = folder.foldOver(underlying)
  }
}
