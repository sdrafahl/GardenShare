package com.gardenShare.gardenshare.GetListOfProductNames

import com.gardenShare.gardenshare.GetDescriptionBucketName
import com.gardenShare.gardenshare.Storage.S3.GetKeys
import com.gardenShare.gardenshare.Storage.S3.GetKeys.GetKeysOps
import com.gardenShare.gardenshare.GetTypeSafeConfig
import cats.effect.IO

case class DescriptionName(underlying: String)
abstract class GetListOfProductNames[F[_]] {
  def getListOfProducts: F[List[DescriptionName]]
}

object GetListOfProductNames {
  def apply[F[_]: GetListOfProductNames]() = implicitly[GetListOfProductNames[F]]

  implicit def createIOGetListOfProductNames(implicit getKeys: GetKeys[IO], getDescBucketName: GetDescriptionBucketName[IO], get: GetTypeSafeConfig[IO]) = new GetListOfProductNames[IO] {
    def getListOfProducts: IO[List[DescriptionName]] = for {
      bucketName <- getDescBucketName.get
      keys <- bucketName.keys      
    } yield keys.map(a => DescriptionName(a.underlying))
  }  
}
