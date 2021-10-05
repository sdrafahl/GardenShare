package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import cats.effect.IO
import cats.implicits._
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.GetStoresStream
import com.gardenShare.gardenshare.GetTypeSafeConfig
import scala.concurrent.ExecutionContext
import org.http4s.server.AuthMiddleware
import cats.effect.Temporal

abstract class GetRoutes[F[_], T <: RoutesTypes] {
  def getRoutes: HttpRoutes[F]
}

object GetRoutes {
  def apply[F[_], T <: RoutesTypes]()(implicit x:GetRoutes[F, T]) = x

  implicit def ioTestingRoutes(
    implicit e: ApplyUserToBecomeSeller[IO],
    g: GetUserInfo[IO],
    gs: GetStore[IO],
    gst: GetStoresStream[IO],
    tsc: GetTypeSafeConfig[IO],
    signUpUser: SignupUser[IO],
    cognitoClient: CogitoClient[IO],
    deleteStore: DeleteStore[IO],
    addProductToStore: AddProductToStore[IO],
    getProductsByStore:GetProductsByStore[IO],
    timer: Temporal[IO],
    verifyUserAsSeller: VerifyUserAsSeller[IO],
    ec: ExecutionContext,
    authMiddleWear: AuthMiddleware[IO, Email]
  ) = new GetRoutes[IO, TestingAndProductionRoutes]{
    def getRoutes: HttpRoutes[IO] = (
      UserRoutes.userRoutes[IO]() <+>
        TestUserRoutes.userRoutes[IO]() <+>
        ProductRoutes.productRoutes[IO] <+>
        StoreRoutes.storeRoutes[IO] <+>
        ProductDescriptionRoutes.productDescriptionRoutes[IO]
    )
  }

  implicit def ioProductionRoutes(
    implicit e: ApplyUserToBecomeSeller[IO],
    g: GetUserInfo[IO],
    gs: GetStore[IO],
    gst: GetStoresStream[IO],
    tsc: GetTypeSafeConfig[IO],
    signUpUser: SignupUser[IO],
    cognitoClient: CogitoClient[IO],
    addProductToStore: AddProductToStore[IO],
    getProductsByStore:GetProductsByStore[IO],
    timer: Temporal[IO],
    verifyUserAsSeller: VerifyUserAsSeller[IO],
    ec: ExecutionContext,
    authMiddleWear: AuthMiddleware[IO, Email]
  ) = new GetRoutes[IO, OnlyProductionRoutes] {
    def getRoutes: HttpRoutes[IO] = UserRoutes.userRoutes[IO]() <+> ProductRoutes.productRoutes[IO] <+> StoreRoutes.storeRoutes[IO]
  }
}
