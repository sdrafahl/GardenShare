package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.GetDistance
import com.gardenShare.gardenshare.GetStoresStream
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.GetNearestStores.GetNearestOps
import cats.implicits._
import org.http4s.circe.CirceEntityCodec._
import ProcessPolymorphicType.ProcessPolymorphicTypeOps
import org.http4s.AuthedRoutes
import org.http4s.server.AuthMiddleware
import cats.effect.Temporal

object StoreRoutes {
  def storeRoutes[F[_]:
      Async:
      GetNearestStores:
      GetStoresStream:
      GetDistance:
      Temporal:
      GetThreadCountForFindingNearestStores:
      ProcessPolymorphicType
  ](implicit authMiddleWear: AuthMiddleware[F, Email]): HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._
    
    authMiddleWear(AuthedRoutes.of[Email, F] {
      case req @ POST -> Root / "store" / Limit(limit) / DistanceInMiles(rangeInMiles) as _ => for {
        address <- req.req.as[Address]
        response <- GetNearestStore(rangeInMiles, limit, address).nearest.map(NearestStores(_)).asJsonF
      } yield response
    })    
  }
}
