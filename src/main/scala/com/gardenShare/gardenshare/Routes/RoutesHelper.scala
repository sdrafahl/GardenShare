package com.gardenShare.gardenshare

import org.http4s.Request
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.UserEntities.JWTValidationTokens
import com.gardenShare.gardenshare._
import org.http4s._
import cats.Functor
import cats.syntax.functor._
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.UserEntities.ValidToken
import cats.Functor
import cats.syntax.functor
import io.circe.fs2._
import io.circe.generic.auto._, io.circe.syntax._
import fs2.text
import com.gardenShare.gardenshare.domain.Store.Address
import cats.effect.IO
import cats.effect.Sync
import com.gardenShare.gardenshare.Encoders.Encoders._
import io.circe.Decoder

object Helpers {
  def parseJWTokenFromRequest[F[_]: Functor](req: Request[F]) = {
    (req
      .headers
      .get(CaseInsensitiveString("authentication"))) match {
      case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.value))
      case None => Left(NoJWTTokenProvided())
    }
  }
  
  def addJsonHeaders[F[_]: Functor](res: F[Response[F]]) = {
    res.map(resp => resp.copy(
      headers = resp.headers.put(
        Header.apply("Content-Type", "application/json")
      )
    ))
  }

  def parseBodyFromRequest[T, F[_]: Sync](req: Request[F])(implicit d: Decoder[T]): F[Option[T]] = {
    req
      .body
      .through(text.utf8Decode)
      .through(stringStreamParser)
      .through(decoder[F, T])
      .compile
      .toList
      .map(_.headOption)
  }
}


