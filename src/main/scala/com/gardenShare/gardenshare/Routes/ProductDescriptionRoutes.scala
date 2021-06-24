package com.gardenShare.gardenshare

import cats.effect.Async
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare._
import com.gardenShare.gardenshare.GetproductDescription._
import ParsingDecodingImplicits.createEncoder

object ProductDescriptionRoutes {
  def productDescriptionRoutes[F[_]: Async]
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "productDescription" / Produce(produce) => Ok(produce.getProductDescription.asJson.toString)
    }
  }
}
