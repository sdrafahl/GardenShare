
package com.gardenShare.gardenshare

import org.http4s.Request
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.JWTValidationTokens
import org.http4s._
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.ValidToken
import cats.Functor
import io.circe.fs2._
import io.circe.generic.auto._, io.circe.syntax._
import fs2.text
import cats.effect.Sync
import io.circe.Decoder
import cats.MonadError
import cats.implicits._
import cats.Applicative
import org.http4s.dsl.Http4sDsl
import org.http4s.Header
import com.gardenShare.gardenshare.Email
import io.circe.Json
import cats.Monad
import com.gardenShare.gardenshare.InvalidToken
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers._

import cats.data.{WriterT, Writer}

import cats.effect.IO

object Helpers {
  val optionWriterT1 : WriterT[Option, String, Int] = WriterT(Some(("writerT value 1", 123)))
  optionWriterT1.flatMap($0)

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

  def parseRequestAndValidateUser[F[_]:AuthUser: AuthJWT:Monad:JoseProcessJwt](req: Request[F] ,validWithEmail: Email => F[Json]) = {
    parseJWTokenFromRequest(req)
      .map(_.auth)
      .map{resultOfValidation =>
        resultOfValidation.flatMap{
          case InvalidToken(msg) => Applicative[F].pure(InvalidToken("Invalid Token was provided").asJson)
          case ValidToken(None) => Applicative[F].pure(InvalidToken("Token is valid but without email").asJson)
          case ValidToken(Some(email)) => validWithEmail(email)
        }
      }.foldIntoJson
  }

  def parseRequestAndValidateUserResponse[F[_]:AuthUser:AuthJWT:Http4sDsl:JoseProcessJwt](req: Request[F] ,validWithEmail: Email => F[Json])(implicit me: MonadError[F, Throwable]) = {
    val dsl = implicitly[Http4sDsl[F]]
    import dsl._
    parseRequestAndValidateUser[F](req, validWithEmail)
      .flatMap(k => Ok(k.toString()))
      .catchError
  }

  def parseREquestAndValidateUserAndParseBody[A:Decoder,F[_]:AuthUser:AuthJWT:Monad:Sync:JoseProcessJwt](req: Request[F], pr: (Email,A) => F[Json]) = {
    parseRequestAndValidateUser[F](req, {email =>
      parseBodyFromRequest[A, F](req)
        .flatMap{
          case None => Applicative[F].pure(ResponseBody("Failed to parse body from the request", false).asJson)
          case Some(a) => pr(email, a)
        }
    })
  }

  def parseREquestAndValidateUserAndParseBodyResponse[A:Decoder,F[_]:AuthUser:AuthJWT:Monad:Sync:Http4sDsl:JoseProcessJwt](req: Request[F], pr: (Email,A) => F[Json])(implicit me: MonadError[F, Throwable]) = {
    val dsl = implicitly[Http4sDsl[F]]
    import dsl._
    parseREquestAndValidateUserAndParseBody[A, F](req, pr)
      .flatMap(k => Ok(k.toString()))
      .catchError
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

  implicit class ResponseHelper[F[_]](resp: F[Response[F]]) {
    def catchError(implicit ae: MonadError[F, Throwable], h: Http4sDsl[F]) = {
      import h._
      resp
        .attempt
        .flatMap(_.fold(err => Ok(ResponseBody(s"There was an error parsing: ${err.getMessage()}", false).asJson.toString()),b => Applicative[F].pure(b)))
    }
  }
}


