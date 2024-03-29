package com.gardenShare.gardenshare

import awscala._, s3._
import cats.effect.IO

case class S3Key(underlying: String)
abstract class GetKeys[F[_]] {
  def getKeys(bucketName: com.gardenShare.gardenshare.Bucket)(implicit client: S3): F[List[S3Key]]
}

object GetKeys {
  implicit object IOGetKeys extends GetKeys[IO] {
    def getKeys(bucketName: com.gardenShare.gardenshare.Bucket)(implicit client: S3): IO[List[S3Key]] = {
      val bucket = IO(client
        .bucket(bucketName.n.value)
        .map(a => a.keys())
        .map(_.toList)
        .map(_.map(a => S3Key(a))))
      bucket.flatMap {
        case Some(keys) => IO(keys)
        case None => IO.raiseError(new Throwable("Bucket does not exist"))
      }
    }
  }
  implicit class GetKeysOps(underlying: com.gardenShare.gardenshare.Bucket) {
    import com.gardenShare.gardenshare.Storage.S3.Clients._
    def keys[F[_]](implicit getKeys:GetKeys[F]) = getKeys.getKeys(underlying)
  }
}
