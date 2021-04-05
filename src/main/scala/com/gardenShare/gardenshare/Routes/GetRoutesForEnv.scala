package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import com.gardenShare.gardenshare.SystemEnvionment
import com.gardenShare.gardenshare.Testing
import com.gardenShare.gardenshare.Production
import cats.effect.IO
import com.gardenShare.gardenshare.GetTypeSafeConfig
import ParsingDecodingImplicits._

abstract class GetRoutesForEnv[F[_]] {
  def getRoutesGivenEnv(env: SystemEnvionment): HttpRoutes[F] 
}

 object GetRoutesForEnv {
  def apply[F[_]: GetRoutesForEnv]() = implicitly[GetRoutesForEnv[F]]

   implicit def createIoGetRoutesForEnv(
     implicit testingGetRoutes: GetRoutes[IO, TestingAndProductionRoutes],
     productionRoutes: GetRoutes[IO, OnlyProductionRoutes],
     tsc: GetTypeSafeConfig[IO]
   ) = new GetRoutesForEnv[IO] {
    def getRoutesGivenEnv(env: SystemEnvionment): HttpRoutes[IO] = env match {
      case Testing() => testingGetRoutes.getRoutes
      case Production() => productionRoutes.getRoutes
    }
  }
}
