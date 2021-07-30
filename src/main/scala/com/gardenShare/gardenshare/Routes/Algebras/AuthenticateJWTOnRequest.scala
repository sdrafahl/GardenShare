package com.gardenShare.gardenshare

import org.http4s.Request
import cats.MonadError

abstract class AuthenticateJWTOnRequest[F[_]] {
  def authenticateRequest(request: Request[F])(implicit me: MonadError[F, Throwable]): F[Email]
}

object AuthenticateJWTOnRequest {
  implicit def createAuthenticateJWTOnRequest[F[_]: AuthJWT]: AuthenticateJWTOnRequest[F] = new AuthenticateJWTOnRequest[F] {
    def authenticateRequest(request: Request[F])(implicit me: MonadError[F, Throwable]): F[Email] = Helpers.validateJWTToken(request)
  }

  implicit class AuthenticateJWTOnRequestOps[F[_]](underlying: Request[F]) {
    def authJWT(implicit authJwtReq: AuthenticateJWTOnRequest[F], me: MonadError[F, Throwable]) = authJwtReq.authenticateRequest(underlying)
  }
}
