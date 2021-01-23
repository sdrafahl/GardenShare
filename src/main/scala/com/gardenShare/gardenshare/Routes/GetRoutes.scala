package com.gardenShare.gardenshare

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import cats.effect.IO
import cats.syntax._
import cats.implicits._

abstract class GetRoutes[F[_], T <: RoutesTypes] {
  def getRoutes: HttpRoutes[F]
}

object GetRoutes {
  def apply[F[_], T <: RoutesTypes]()(implicit x:GetRoutes[F, T]) = x

  implicit object IOTestingRoutes extends GetRoutes[IO, TestingAndProductionRoutes]{
    def getRoutes: HttpRoutes[IO] = (
      UserRoutes.userRoutes[IO]() <+>
      TestUserRoutes.userRoutes[IO]()
    )
  }

  implicit object IOProductionRoutes extends GetRoutes[IO, OnlyProductionRoutes] {
    def getRoutes: HttpRoutes[IO] = UserRoutes.userRoutes[IO]()
  }
}
