package com.gardenShare.gardenshare.GetListOfProductNames

import com.gardenShare.gardenshare.Storage.S3.GetKeys
import cats.effect.IO
import com.gardenShare.gardenshare.GetDescriptionBucketName
import com.gardenShare.gardenshare.Storage.S3.GetKeys
import com.gardenShare.gardenshare.Storage.S3.GetKeys.GetKeysOps
import com.gardenShare.gardenshare.GetTypeSafeConfig
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.FlatMap
import cats.Functor

case class DescriptionName(underlying: String)
abstract class GetListOfProductNames[F[_]] {
  def getListOfProducts: F[List[DescriptionName]]
}

object GetListOfProductNames {
  def apply[F[_]: GetListOfProductNames]() = implicitly[GetListOfProductNames[F]]

  implicit def createGetListOfProductNames[F[_]: FlatMap:Functor](implicit getKeys: GetKeys[F], getDescBucketName: GetDescriptionBucketName[F], get: GetTypeSafeConfig[F]) = new GetListOfProductNames[F] {
    def getListOfProducts: F[List[DescriptionName]] = for {
      bucketName <- getDescBucketName.get
      keys <- bucketName.keys      
    } yield keys.map(a => DescriptionName(a.underlying))
  }  
}
