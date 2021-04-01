package com.gardenShare.gardenshare

import cats.effect.Async
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.InsertStore
import com.gardenShare.gardenshare.GetDistance
import com.gardenShare.gardenshare.GetStoresStream
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.JWTValidationTokens
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthUser._
import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.AuthJWT._
import cats.implicits._
import com.gardenShare.gardenshare.InvalidToken
import com.gardenShare.gardenshare.ValidToken
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.CreateStoreRequest
import com.gardenShare.gardenshare.InsertStore.CreateStoreRequestOps
import com.gardenShare.gardenshare.InsertStore
import scala.util.Try
import com.gardenShare.gardenshare.GetNearestStores.GetNearestOps
import com.gardenShare.gardenshare.Helpers._
import cats.Applicative
import com.gardenShare.gardenshare.ProcessAndJsonResponse
import com.gardenShare.gardenshare.ProcessAndJsonResponse._
import com.gardenShare.gardenshare.ProcessData
import com.gardenShare.gardenshare.Store
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers._
import com.gardenShare.gardenshare.Helpers.ResponseHelper
import cats.effect.ContextShift
import cats.effect.Timer
import EmailCompanion._

object StoreRoutes {
  def storeRoutes[F[_]:
      Async:
      com.gardenShare.gardenshare.SignupUser:
      AuthUser:
      AuthJWT:
      InsertStore:
      GetNearestStores:
      GetStoresStream:
      GetDistance:
      ContextShift:
      Timer:
      GetThreadCountForFindingNearestStores:
      JoseProcessJwt
  ](implicit d: Decoder[Address], e: Encoder[Address])
      : HttpRoutes[F] = {
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
            parseREquestAndValidateUserAndParseBodyResponse[Address, F](req, {(email, address) =>
              ProcessData(
                GetNearestStore(DistanceInMiles(range), limit, address).nearest,
                (lst: List[RelativeDistanceAndStore]) => NearestStores(lst),
                (err:Throwable) => ResponseBody("Error finding stores", false)
              ).process
            })
          }
        }
      }
    }
  }
}
