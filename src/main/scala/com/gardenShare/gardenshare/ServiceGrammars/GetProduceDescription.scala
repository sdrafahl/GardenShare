package com.gardenShare.gardenshare

import cats.effect.IO

abstract class GetProduceDescription[F[_]] {
  def get(produce: Produce): F[ProductDescription]
}

object GetProduceDescription {
  implicit object IOGetProduceDescription extends GetProduceDescription[IO] {
    def get(produce: Produce): IO[ProductDescription] = produce match {
      case Produce.BrownOysterMushrooms => IO.pure(ProductDescription("BrownOysterMushrooms", PriceUnit.Pound, produce))
    }
  }
}


