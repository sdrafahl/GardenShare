package com.gardenShare.gardenshare.domain.Store

import com.gardenShare.gardenshare.UserEntities._

case class Address(underlying: String)
case class Store(id: Int, address: Address, sellerEmail: Email)
case class CreateStoreRequest(address: Address, sellerEmail: Email)
