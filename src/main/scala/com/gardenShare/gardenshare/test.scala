package com.gardenShare.gardenshare

import cats.effect.Async

object Test {
  def te[F[_]: Async] = {
    implicitly[Async[F]]
    ???
  }
}
