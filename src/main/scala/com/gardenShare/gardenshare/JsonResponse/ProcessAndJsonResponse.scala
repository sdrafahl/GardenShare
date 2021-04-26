package com.gardenShare.gardenshare

import io.circe.syntax._
import io.circe._
import cats.ApplicativeError
import cats.implicits._
import com.gardenShare.gardenshare.ProcessData

abstract class ProcessAndJsonResponse {
  def process[F[_], D, E: Encoder,G: Encoder](a: F[D], op: D => E, errOp: Throwable => G)(implicit e: ApplicativeError[F, Throwable]): F[Json]
}

object ProcessAndJsonResponse {
  implicit object DefaultProcessAndJsonResponse extends ProcessAndJsonResponse {
    def process[F[_], D, E: Encoder,G: Encoder](a: F[D], op: D => E, errOp: Throwable => G)(implicit e: ApplicativeError[F, Throwable]): F[Json] = {
      a
        .map(op)
        .attempt
        .map{
          case Right(s) => s.asJson
          case Left(e) => errOp(e).asJson
        }        
    }
  }
  implicit class ProcessAndJsonResponseOps[F[_], D, E: Encoder,G: Encoder](underlying: ProcessData[F,D,E,G]) {
    def process(implicit p: ProcessAndJsonResponse, e: ApplicativeError[F, Throwable]) = p.process(underlying.data, underlying.op, underlying.errOp)
  }
}





