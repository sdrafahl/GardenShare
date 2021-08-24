package com.gardenShare.gardenshare

import cats.data.Kleisli
import cats.data.OptionT
import org.http4s.Request
import AuthenticateJWTOnRequest.AuthenticateJWTOnRequestOps
import cats.MonadError
import org.http4s.server.AuthMiddleware

object AuthMiddleWear {
  type AuthMiddleWearTransformation[F[_]] = Kleisli[OptionT[F, *], Request[F], Email]

   def createMiddleWear[F[_]: AuthenticateJWTOnRequest](implicit me: MonadError[F, Throwable]): AuthMiddleWearTransformation[F] = Kleisli({ request =>
     OptionT.liftF(request.authJWT)
   })


  implicit def createHttp4sMiddlewear[F[_]: AuthenticateJWTOnRequest](implicit me: MonadError[F, Throwable]): AuthMiddleware[F, Email] = AuthMiddleware(createMiddleWear[F])
}
