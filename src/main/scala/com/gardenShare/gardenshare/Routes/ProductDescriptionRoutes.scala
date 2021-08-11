package com.gardenShare.gardenshare

import cats.effect.Async
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import io.circe.generic.auto._
import com.gardenShare.gardenshare._
import ProcessPolymorphicType.ProcessPolymorphicTypeOps

object ProductDescriptionRoutes {
  def productDescriptionRoutes[F[_]: Async: ProcessPolymorphicType]
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "productDescription" / Produce(produce) => implicitly[Async[F]].pure(produce).asJsonF
    }
  }
}
