package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities.Email
import java.time.ZonedDateTime

case class ProductAndQuantity(product: ProductWithId, quantity: Int)
case class StoreOrderRequest(seller: Email, buyer: Email, products: List[ProductAndQuantity], dateSubmitted: ZonedDateTime)
case class StoreOrderRequestWithId(id: Int, storeOrderRequest: StoreOrderRequest)
