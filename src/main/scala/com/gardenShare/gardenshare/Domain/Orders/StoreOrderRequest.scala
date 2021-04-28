package com.gardenShare.gardenshare

import java.time.ZonedDateTime

case class StoreOrderRequest(seller: Email, buyer: Email, products: List[ProductAndQuantity], dateSubmitted: ZonedDateTime)
