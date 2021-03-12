package com.gardenShare.gardenshare

import fs2.Stream
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.string.NonEmptyString

case class BucketN(n: NonEmptyString)
case class BucketK(k: NonEmptyString)
case class GetStreamFromS3(name: BucketN, key: BucketK)
case class LazyStream[F[_], A](s: F[Stream[F, A]])
