package com.gardenShare.gardenshare

import org.http4s.Request
import cats.MonadError
import cats.implicits._
import org.typelevel.ci.CIString
import AuthJWT.AuthJwtOps
import JWTValidationResult.{InvalidToken, ValidToken}

abstract class AuthenticateJWTOnRequest[F[_]] {
  def authenticateRequest(request: Request[F])(implicit me: MonadError[F, Throwable]): F[Email]
}

object AuthenticateJWTOnRequest {
  implicit def createAuthenticateJWTOnRequest[F[_]: AuthJWT]: AuthenticateJWTOnRequest[F] = new AuthenticateJWTOnRequest[F] {
    def authenticateRequest(request: Request[F])(implicit me: MonadError[F, Throwable]): F[Email] = {
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
  }

  private[this] def parseJWTokenFromRequest[F[_]](req: Request[F]) = {
    (req
      .headers
      .get(CIString("authentication"))) match {
      case Some(jwtToken) => Right(JWTValidationTokens(jwtToken.head.value))
      case None => Left(NoJWTTokenProvided())
    }
  }



  implicit class AuthenticateJWTOnRequestOps[F[_]](underlying: Request[F]) {
    def authJWT(implicit authJwtReq: AuthenticateJWTOnRequest[F], me: MonadError[F, Throwable]) = authJwtReq.authenticateRequest(underlying)
  }
}
