package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.GetTypeSafeConfig
import com.gardenShare.gardenshare._
import cats.effect.IO
import GetKeys.GetKeysOps

abstract class GetListOfProductNames[F[_]] {
  def getListOfProducts: F[List[DescriptionName]]
}

object GetListOfProductNames {
  def apply[F[_]: GetListOfProductNames]() = implicitly[GetListOfProductNames[F]]

  implicit def createIOGetListOfProductNames(implicit getKeys: GetKeys[IO], getDescBucketName: GetDescriptionBucketName[IO], get: GetTypeSafeConfig[IO]): GetListOfProductNames[IO]  = new GetListOfProductNames[IO] {
    def getListOfProducts: IO[List[DescriptionName]] = for {
      bucketName <- getDescBucketName.get
      keys <- bucketName.keys      
    } yield keys.map(a => DescriptionName(a.underlying))
  }  
}
