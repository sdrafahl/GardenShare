package com.gardenShare.gardenshare

import cats.effect.IO
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.Response
import org.http4s.dsl.io._

abstract class ProcessPolymorphicType[F[_]] {
  def toJsonInF[A](polyType: F[A])(implicit encoder: Encoder[A]): F[Response[F]]
}

object ProcessPolymorphicType {
  implicit object IOProcessPolymorphicType extends ProcessPolymorphicType[IO] {
    def toJsonInF[A](polyType: IO[A])(implicit encoder: Encoder[A]): IO[Response[IO]] = {      
      for {
        attemptedPolyType  <- polyType.attempt
        jsonResponse = attemptedPolyType match {
          case Left(err) => ErrorResponse(s"Error running request ${err.getMessage()}").asJson
          case Right(a) => a.asJson
        }
        responseToClient <- Ok(jsonResponse.toString())
      } yield responseToClient        
    }
  }

  implicit class ProcessPolymorphicTypeOps[F[_], A](underlying: F[A]) {
    def asJsonF(implicit process: ProcessPolymorphicType[F], encoder: Encoder[A]): F[Response[F]] = process.toJsonInF(underlying)
  }
}
