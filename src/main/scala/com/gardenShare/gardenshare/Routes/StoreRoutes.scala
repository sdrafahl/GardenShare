package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.GetDistance
import com.gardenShare.gardenshare.GetStoresStream
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.GetNearestStores.GetNearestOps
import cats.effect.ContextShift
import cats.effect.Timer
import cats.implicits._
import AuthenticateJWTOnRequest.AuthenticateJWTOnRequestOps
import org.http4s.circe.CirceEntityCodec._
import ProcessPolymorphicType.ProcessPolymorphicTypeOps

object StoreRoutes {
  def storeRoutes[F[_]:
      Async:
      AuthJWT:
      GetNearestStores:
      GetStoresStream:
      GetDistance:
      ContextShift:
      Timer:
      GetThreadCountForFindingNearestStores:
      ProcessPolymorphicType
  ]: HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._      
    HttpRoutes.of[F] {
      case req @ POST -> Root / "store" / Limit(limit) / DistanceInMiles(rangeInMiles) => {
        for {
          _ <- req.authJWT
          address <- req.as[Address]
          response <- GetNearestStore(rangeInMiles, limit, address).nearest.map(NearestStores(_)).asJsonF
        } yield response        
      }
    }
  }
}
