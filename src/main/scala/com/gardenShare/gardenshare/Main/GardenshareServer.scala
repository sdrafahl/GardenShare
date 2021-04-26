package com.gardenShare.gardenshare

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.SignupUser
import com.gardenShare.gardenshare.GetTypeSafeConfig
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.GetUserPoolSecret
import com.gardenShare.gardenshare.AuthUser
import com.gardenShare.gardenshare.GetUserPoolId
import com.gardenShare.gardenshare.AuthJWT
import com.gardenShare.gardenshare.GetRegion
import com.gardenShare.gardenshare.HttpsJwksBuilder
import org.http4s.server.middleware._
import scala.concurrent.duration._
import com.gardenShare.gardenshare.InsertStore
import com.gardenShare.gardenshare.GetDistance
import com.gardenShare.gardenshare.GetStoresStream
import com.gardenShare.gardenshare.Storage.S3.GetKeys
import com.gardenShare.gardenshare.GetDescriptionBucketName
import com.gardenShare.gardenshare.InsertProduct
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetStoreByID
import com.gardenShare.gardenshare.GetEnvironment
import scala.concurrent.ExecutionContext

object GardenshareServer {

  def stream[F[_]:
      ConcurrentEffect:
      CogitoClient:
      GetTypeSafeConfig:
      SignupUser:
      GetUserPoolName:            
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder:
      InsertStore:
      GetDistance:
      GetStoresStream:
      GetKeys:
      GetDescriptionBucketName:
      InsertProduct:
      GetStore:
      GetStoreByID:
      GetEnvironment:
      GetRoutesForEnv
  ](implicit T: Timer[F], C: ContextShift[F], ec: ExecutionContext): F[Unit] = {

    // implicit val config = getTypeSafeConfig
    // implicit val signupUser = SignupUser[F]()

    GetEnvironment().getEnv.flatMap { sysEnv =>
      val httpApp = (
          GetRoutesForEnv().getRoutesGivenEnv(sysEnv)
      ).orNotFound

      val finalHttpApp = Logger.httpApp(true, true)(httpApp)

      val methodConfig = CORSConfig(
          anyOrigin = true,
          anyMethod = true,
          allowCredentials = true,
        maxAge = 1.day.toSeconds)

      val corsService = CORS(finalHttpApp, methodConfig)

      (for {

        exitCode <- BlazeServerBuilder[F]
        .bindHttp(8055, "0.0.0.0")
        .withHttpApp(corsService)
        .enableHttp2(true)
        .serve
      } yield exitCode).drain.compile.drain
    }
  }
}
