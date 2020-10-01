package com.gardenShare.gardenshare.GetListOfProductNames

import com.gardenShare.gardenshare.Storage.S3.GetKeys
import cats.effect.IO
import com.gardenShare.gardenshare.Config.GetDescriptionBucketName
import com.gardenShare.gardenshare.Storage.S3.GetKeys
import com.gardenShare.gardenshare.Storage.S3.GetKeys._

case class DescriptionName(underlying: String)
abstract class GetListOfProductNames[F[_]] {
  def getListOfProducts(implicit getKeys: GetKeys[F], getDescBucketName: GetDescriptionBucketName[F]): F[List[DescriptionName]]
}

object GetListOfProductNames {
  def apply[F[_]: GetListOfProductNames]() = implicitly[GetListOfProductNames[F]]
  implicit object IOGetListOfProductNames extends GetListOfProductNames[IO] {
    def getListOfProducts(implicit getKeys: GetKeys[IO], getDescBucketName: GetDescriptionBucketName[IO]): IO[List[DescriptionName]] = for {
      bucketName <- getDescBucketName.get
      keys <- bucketName.keys      
    } yield keys.map(a => DescriptionName(a.underlying))
  }
}
