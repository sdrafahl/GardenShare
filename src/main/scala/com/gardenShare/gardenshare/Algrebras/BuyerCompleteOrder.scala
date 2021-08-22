package com.gardenShare.gardenshare

abstract class BuyerCompleteOrder[F[_]] {
  def completeOrder(orderid: OrderId, buyerEmail: Email): F[Unit]
}
