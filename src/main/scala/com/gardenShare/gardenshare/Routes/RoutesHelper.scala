package com.gardenShare.gardenshare

import org.http4s.Request
import org.http4s.util.CaseInsensitiveString
import com.gardenShare.gardenshare.JWTValidationTokens
import org.http4s._
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.AuthJWT.AuthJwtOps
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
import com.gardenShare.gardenshare.FoldOver.FoldOverEithers._
import JWTValidationResult._

object Helpers {
  def validateJWTToken[F[_]: AuthJWT](request: Request[F])(implicit me: MonadError[F, Throwable]): F[Email] = {    
    val asbc: Either[NoJWTTokenProvided, JWTValidationTokens] = parseJWTokenFromRequest(request)
    val throwableAsbc = asbc.left.map(_ => new Throwable("No JWT token provided"))
    val jwtPgm: F[JWTValidationTokens] = me.fromEither(throwableAsbc)
    for {
      jwtToken <- jwtPgm
      jwtTokenValidationResult <- jwtToken.auth
      emailForJwt <- jwtTokenValidationResult match {
        case InvalidToken(msg) => me.raiseError[Email](new Throwable(msg))
        case ValidToken(None) => me.raiseError[Email](new Throwable("Token is valid but without email"))
        case ValidToken(Some(email)) => me.pure(email)
      }
    } yield emailForJwt
  }

  def parseJWTokenFromRequest[F[_]](req: Request[F]) = {
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

  def parseRequestAndValidateUser[F[_]: AuthJWT:Monad](req: Request[F] ,validWithEmail: Email => F[Json]) = {
    parseJWTokenFromRequest(req)
      .map(_.auth)
      .map{resultOfValidation =>
        resultOfValidation.flatMap{
          case InvalidToken(_) => Applicative[F].pure(InvalidToken("Invalid Token was provided").asJson)
          case ValidToken(None) => Applicative[F].pure(InvalidToken("Token is valid but without email").asJson)
          case ValidToken(Some(email)) => validWithEmail(email)
        }
      }.foldIntoJson
  }

  def parseRequestAndValidateUserResponse[F[_]:AuthJWT:Http4sDsl](req: Request[F] ,validWithEmail: Email => F[Json])(implicit me: MonadError[F, Throwable]) = {
    val dsl = implicitly[Http4sDsl[F]]
    import dsl._
    parseRequestAndValidateUser[F](req, validWithEmail)
      .flatMap(k => Ok(k.toString()))
      .catchError
  }

  def parseREquestAndValidateUserAndParseBody[A:Decoder,F[_]:AuthJWT:Sync](req: Request[F], pr: (Email,A) => F[Json]) = {
    parseRequestAndValidateUser[F](req, {email =>
      parseBodyFromRequest[A, F](req)
        .flatMap{
          case None => Applicative[F].pure(ResponseBody("Failed to parse body from the request", false).asJson)
          case Some(a) => pr(email, a)
        }
    })
  }

  def parseREquestAndValidateUserAndParseBodyResponse[A:Decoder,F[_]:AuthJWT:Sync:Http4sDsl](req: Request[F], pr: (Email,A) => F[Json]) = {
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


