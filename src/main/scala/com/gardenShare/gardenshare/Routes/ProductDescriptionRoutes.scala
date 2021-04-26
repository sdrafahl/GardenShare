package com.gardenShare.gardenshare

import cats.effect.Async
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare._
import com.gardenShare.gardenshare.GetproductDescription
import com.gardenShare.gardenshare.GetproductDescription._
import com.gardenShare.gardenshare.Parser

object ProductDescriptionRoutes {
  def productDescriptionRoutes[F[_]: Async](implicit c: GetproductDescription[Produce], p: Parser[Produce])
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "productDescription" / descKey => {        
        p.parse(descKey)
          .map(a => a.getProductDescription)
          .fold(a => Ok(ResponseBody("Invalid product description key was provided.", false).asJson.toString), b => Ok(b.asJson.toString))
      }
    }
  }
}
