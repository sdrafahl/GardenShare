package com.gardenShare.gardenshare

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import io.circe.generic.auto._
import com.gardenShare.gardenshare._
import ProcessPolymorphicType.ProcessPolymorphicTypeOps
import cats.Defer
import cats.Applicative
import cats.effect.kernel.Async

object ProductDescriptionRoutes {
  def productDescriptionRoutes[F[_]: ProcessPolymorphicType: GetProduceDescription: Defer: Applicative: Async]
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "productDescription" / Produce(produce) => implicitly[GetProduceDescription[F]].get(produce).asJsonF
    }
  }
}
