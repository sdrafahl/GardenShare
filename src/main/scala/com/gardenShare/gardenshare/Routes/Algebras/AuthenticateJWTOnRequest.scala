package com.gardenShare.gardenshare

import org.http4s.Request
import cats.MonadError

abstract class AuthenticateJWTOnRequest[F[_]] {
  def authenticateRequest(request: Request[F]): F[Email]
}

object AuthenticateJWTOnRequest {
  implicit def createAuthenticateJWTOnRequest[F[_]]:AuthenticateJWTOnRequest[F]  = new AuthenticateJWTOnRequest {
    def authenticateRequest(request: Request[F])(implicit me: MonadError[F, Throwable]): F[Email] = Helpers.validateJWTToken(request)
  }

  implicit class AuthenticateJWTOnRequestOps[F[_]](underlying: Request[F]) {
    def authJWT(implicit authJwtReq: AuthenticateJWTOnRequest[F]) = authJwtReq.authenticateRequest(underlying)
  }
}
