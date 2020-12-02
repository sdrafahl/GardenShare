package com.gardenShare.gardenshare

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import com.gardenShare.gardenshare.SignupUser.SignupUser
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetUserPoolSecret
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser._
import com.gardenShare.gardenshare.authenticateUser.AuthUser.AuthUser
import com.gardenShare.gardenshare.Config.GetUserPoolId
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.AuthJWT
import com.gardenShare.gardenshare.Config.GetRegion
import com.gardenShare.gardenshare.authenticateUser.AuthJWT.HttpsJwksBuilder
import org.http4s.server.middleware._
import scala.concurrent.duration._
import com.gardenShare.gardenshare.Storage.Relational.InsertStore
import com.gardenShare.gardenshare.GoogleMapsClient.GetDistance
import com.gardenShare.gardenshare.Storage.Relational.GetStoresStream
import com.gardenShare.gardenshare.GetListOfProductNames.GetListOfProductNames
import com.gardenShare.gardenshare.Storage.S3.GetKeys
import com.gardenShare.gardenshare.Config.GetDescriptionBucketName
import com.gardenShare.gardenshare.Storage.Relational.InsertProduct
import com.gardenShare.gardenshare.StoreRoutes
import com.gardenShare.gardenshare.Storage.Relational.GetProductsByStore
import com.gardenShare.gardenshare.ParseDescription.ParseDescriptionStream
import com.gardenShare.gardenshare.GetProductDescription.GetproductDescription
import com.gardenShare.gardenshare.Orders.CreateOrder
import com.gardenShare.gardenshare.Storage.Relational.AddOrderIdToProduct
import com.gardenShare.gardenshare.Storage.Relational.GetProductByID
import com.gardenShare.gardenshare.Storage.Relational.GetStore._
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import com.gardenShare.gardenshare.Storage.Relational.GetStoreByID

object GardenshareServer {

  def stream[F[_]:
      ConcurrentEffect:
      CogitoClient:
      GetUserPoolName:
      GetTypeSafeConfig:
      SignupUser:
      GetUserPoolSecret:
      AuthUser:
      GetUserPoolId:
      AuthJWT:
      GetRegion:
      HttpsJwksBuilder:
      InsertStore:
      GetNearestStores:
      GetDistance:
      GetStoresStream:
      GetListOfProductNames:
      GetKeys:
      GetDescriptionBucketName:
      InsertProduct:
      GetProductsByStore:
      ParseDescriptionStream:
      GetproductDescription:
      CreateOrder:
      AddOrderIdToProduct:
      GetProductByID:
      GetStore:
      GetStoreByID
  ](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).stream

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      
      httpApp = (                
          UserRoutes.userRoutes[F]() <+>
          StoreRoutes.storeRoutes[F]() <+>
          ProductRoutes.productRoutes[F]() <+>
          ProductDescription.productDescriptionRoutes[F]() <+>
          OrderRoutes.orderRoutes[F]()
      ).orNotFound      

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      methodConfig = CORSConfig(
        anyOrigin = true,
        anyMethod = true,
        allowCredentials = true,
        maxAge = 1.day.toSeconds)

      corsService = CORS(finalHttpApp, methodConfig)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")      
        .withHttpApp(corsService)       
        .serve
    } yield exitCode
  }.drain
}
