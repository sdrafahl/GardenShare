package com.gardenShare.gardenshare.domain.Store

case class Address(underlying: String)
case class Email(underlying: String)
case class Store(id: Int, address: Address, sellerEmail: Email)
case class CreateStoreRequest(address: Address, sellerEmail: Email)
