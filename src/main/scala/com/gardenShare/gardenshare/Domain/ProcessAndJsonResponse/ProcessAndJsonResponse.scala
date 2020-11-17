package com.gardenShare.gardenshare.domain.ProcessAndJsonResponse

import cats.Functor
import io.circe.Encoder

case class ProcessData[F[_]: Functor, D, E: Encoder,G: Encoder](data: F[D], op: D => E, errOp: Throwable => G)
