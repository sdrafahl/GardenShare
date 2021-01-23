package com.gardenShare.gardenshare

import io.circe._, io.circe.parser._
import cats.effect.Async
import cats.effect.ContextShift
import org.http4s.Request
import cats.ApplicativeError
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import cats.implicits._
import io.circe.generic.auto._, io.circe.syntax._
import org.http4s.Header
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser.AuthUserOps
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT.AuthJwtOps
import com.gardenShare.gardenshare.Helpers._
import cats.implicits._
import com.gardenShare.gardenshare.UserEntities._
import com.gardenShare.gardenshare.ProcessAndJsonResponse
import com.gardenShare.gardenshare.ProcessAndJsonResponse.ProcessAndJsonResponseOps
import com.gardenShare.gardenshare.domain.ProcessAndJsonResponse.ProcessData
import cats.Applicative


object WorkerRoutes {
  def workerRoutes[F[_]: Async: AuthJWT: CreateWorker](implicit en: Encoder[CreateWorkerResponse]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case req @ POST -> Root / "worker" / "signup" => {
        parseJWTokenFromRequest(req)
          .map{jwtToken =>
            jwtToken.auth.map {
              case InvalidToken(msg) => Left(InvalidToken(msg).asJson.toString())
              case ValidToken(None) => Left(InvalidToken(s"Token is valid but without email").asJson.toString())
              case ValidToken(Some(email)) => {
                Right(ProcessData(
                  CreateWorker().createWorker(Email(email)),
                  (response: CreateWorkerResponse) => response,
                  (err:Throwable) => WorkerFailedToCreate(err.getMessage())
                ).process)
              }
            }
          }.left.map(er => Ok(er.asJson.toString()))
          .map{mbyRespPgm =>
            mbyRespPgm
              .flatMap{eti =>
                eti
                  .left
                  .map(mess => Ok(mess))
                  .map(b => b.flatMap(bb => Ok(bb.toString())))
                  .fold(a => a, b => b)
              }

          }
          .fold(a => a, b => b)
          .attempt
          .flatMap{bc =>
            bc
              .left
              .map{errT => Ok(ResponseBody(errT.getMessage()).asJson.toString())}
              .map(r => Applicative[F].pure(r))
              .fold(b => b, c => c)
          }
      }
    }
  }
}
