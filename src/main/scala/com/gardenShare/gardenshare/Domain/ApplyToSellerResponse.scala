package com.gardenShare.gardenshare.domain

abstract class SellerResponse
case class SellerRequestSuccessful() extends SellerResponse
case class SellerRequestFailed(msg: String) extends SellerResponse

