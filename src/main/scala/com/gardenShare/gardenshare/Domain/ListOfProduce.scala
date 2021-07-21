package com.gardenShare.gardenshare

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class ListOfProduce(listOfProduce: List[ProductWithId])

object ListOfProduce {
  implicit lazy final val ListOfProduceCodec: Codec[ListOfProduce] = deriveCodec
}
