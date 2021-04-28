package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import cats.effect.IO
import cats.implicits._
import com.gardenShare.gardenshare.Shows._ 
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.InsertStore
import io.circe.Decoder
import cats.effect.ContextShift
import com.gardenShare.gardenshare.GetStoresStream
import com.gardenShare.gardenshare.Address
import io.circe.Encoder
import com.gardenShare.gardenshare.GetTypeSafeConfig
import cats.effect.Timer
import scala.concurrent.ExecutionContext

abstract class GetRoutes[F[_], T <: RoutesTypes] {
  def getRoutes: HttpRoutes[F]
}

object GetRoutes {
  def apply[F[_], T <: RoutesTypes]()(implicit x:GetRoutes[F, T]) = x

  implicit def ioTestingRoutes(
    implicit e: ApplyUserToBecomeSeller[IO],
    g: GetUserInfo[IO],
    cs: ContextShift[IO],
    gs: GetStore[IO],
    dec: Decoder[Currency],
    insertStore: InsertStore[IO],
    gst: GetStoresStream[IO],
    addressDecoder: Decoder[Address],
    addressEncoder: Encoder[Address],
    dep: Decoder[Produce],
    produceEncoder: Encoder[Produce],
    currencyEncoder: Encoder[Currency],
    tsc: GetTypeSafeConfig[IO],
    signUpUser: SignupUser[IO],
    cognitoClient: CogitoClient[IO],
    deleteStore: DeleteStore[IO],
    insertProduct: InsertProduct[IO],
    addProductToStore: AddProductToStore[IO],
    getProductsByStore:GetProductsByStore[IO],
    timer: Timer[IO],
    verifyUserAsSeller: VerifyUserAsSeller[IO],
    ec: ExecutionContext
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
    cs: ContextShift[IO],
    gs: GetStore[IO],
    dec: Decoder[Currency],
    insertStore: InsertStore[IO],
    gst: GetStoresStream[IO],
    addressDecoder: Decoder[Address],
    addressEncoder: Encoder[Address],
    dep: Decoder[Produce],
    produceEncoder: Encoder[Produce],
    currencyEncoder: Encoder[Currency],
    tsc: GetTypeSafeConfig[IO],
    signUpUser: SignupUser[IO],
    cognitoClient: CogitoClient[IO],
    insertProduct: InsertProduct[IO],
    addProductToStore: AddProductToStore[IO],
    getProductsByStore:GetProductsByStore[IO],
    timer: Timer[IO],
    verifyUserAsSeller: VerifyUserAsSeller[IO],
    ec: ExecutionContext
  ) = new GetRoutes[IO, OnlyProductionRoutes] {
    def getRoutes: HttpRoutes[IO] = UserRoutes.userRoutes[IO]() <+> ProductRoutes.productRoutes[IO] <+> StoreRoutes.storeRoutes[IO]
  }
}
