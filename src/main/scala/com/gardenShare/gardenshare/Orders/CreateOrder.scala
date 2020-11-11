package com.gardenShare.gardenshare.Orders

import com.gardenShare.gardenshare.domain.Orders._
import com.gardenShare.gardenshare.Storage.Relational.Order
import cats.effect.IO
import com.gardenShare.gardenshare.Storage.Relational.CreateOrderInDB
import com.gardenShare.gardenshare.Storage.Relational.AddOrderIdToProduct
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial
import com.gardenShare.gardenshare.Storage.Relational._
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import cats.effect._
import cats.Parallel._
import cats.ApplicativeError
import cats.FlatMap._
import cats.FlatMap
import cats.Functor
import cats.Functor._
import cats.Parallel
import cats.implicits._

abstract class CreateOrder[F[_]] {
  def createOrder(a: CreateOrderCommand)(implicit addOrderIdToProduct: AddOrderIdToProduct[F], cs: ContextShift[F], getP: GetProductByID[F]): F[Order]
}

object CreateOrder {
  def apply[F[_]:CreateOrder]() = implicitly[CreateOrder[F]]

  implicit def createOrder[F[_]: FlatMap:Functor:Async:Parallel](implicit createOrderInDB: CreateOrderInDB[F], appError: ApplicativeError[F, Throwable]) = new CreateOrder[F] {
    def createOrder(a: CreateOrderCommand)(implicit addOrderIdToProduct: AddOrderIdToProduct[F], cs: ContextShift[F], getP: GetProductByID[F]): F[Order] = {
      for {
        order <- createOrderInDB.createOrder(OrderInit())
        maybeOldProds <- a.productIds.map(i => getP.getProduct(i)).parSequence
        maybeProds = maybeOldProds.collect { case Some(a) => a }
        newProducts <- maybeProds.find(pro => pro.orderId != -1) match {
          case Some(b) if (maybeProds.length == maybeOldProds.length) => a.productIds.map(prodId => addOrderIdToProduct.add(order.orderId, prodId)).parSequence
          case None =>  appError.raiseError(new Throwable("Product does not exist or is already part of an order"))
        }        
      } yield order
    }
  }

  implicit class CreateOrderOps(underlying: CreateOrderCommand) {
    def create[F[_]: CreateOrder:AddOrderIdToProduct: GetProductByID:GetStore:GetStoreByID: ContextShift](implicit crt: CreateOrder[F]) = crt.createOrder(underlying)
  }
}
