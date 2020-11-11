package com.gardenShare.gardenshare.domain.Orders

import com.gardenShare.gardenshare.Storage.Relational.OrderState

case class CreateOrderCommand(productIds: List[Int])
