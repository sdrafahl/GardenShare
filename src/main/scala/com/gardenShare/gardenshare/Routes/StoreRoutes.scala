package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.GetDistance
import com.gardenShare.gardenshare.GetStoresStream
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.AuthUser
import io.circe._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.Address
import scala.util.Try
import com.gardenShare.gardenshare.GetNearestStores.GetNearestOps
import com.gardenShare.gardenshare.Helpers._
import com.gardenShare.gardenshare.ProcessAndJsonResponse._
import com.gardenShare.gardenshare.ProcessData
import cats.effect.ContextShift
import cats.effect.Timer

object StoreRoutes {
  def storeRoutes[F[_]:
      Async:
      AuthUser:
      AuthJWT:
      GetNearestStores:
      GetStoresStream:
      GetDistance:
      ContextShift:
      Timer:
      GetThreadCountForFindingNearestStores:
      JoseProcessJwt
  ]: HttpRoutes[F] = {
    implicit val dsl = new Http4sDsl[F] {}
    import dsl._      
    HttpRoutes.of[F] {
      case req @ POST -> Root / "store" / limit / rangeInMiles => {
        val maybeLimit = Try(limit.toInt).toEither.left
          .map(a => InvalidLimitProvided(a.getMessage()))

        val maybeRange = Try(rangeInMiles.toFloat).toEither.left
          .map(a => InvalidRangeProvided(a.getMessage()))

        (maybeLimit, maybeRange) match {
          case (Left(_), _) => Ok(ResponseBody("There was a error parsing limit", false).asJson.toString())
          case (_, Left(_)) => Ok(ResponseBody("There was a error parsing the range", false).asJson.toString())
          case (Right(limit), Right(range)) => {
            parseREquestAndValidateUserAndParseBodyResponse[Address, F](req, {(_, address) =>
              ProcessData(
                GetNearestStore(DistanceInMiles(range), limit, address).nearest,
                (lst: List[RelativeDistanceAndStore]) => NearestStores(lst),
                (_) => ResponseBody("Error finding stores", false)
              ).process
            })
          }
        }
      }
    }
  }
}
