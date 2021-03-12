package com.gardenShare.gardenshare.Storage.S3

import cats.effect.IO
import software.amazon.awssdk.services.s3.S3Client
import com.gardenShare.gardenshare.GetRegion
import com.gardenShare.gardenshare.Region
import com.gardenShare.gardenshare.GetAwsRegionFromMyRegion
import com.gardenShare.gardenshare.GetAwsRegionFromMyRegion._
import cats.implicits._
import cats.Functor
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import awscala._, s3._
import fs2.{io, text, Stream}
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.BucketN

case class S3Key(underlying: String)
abstract class GetKeys[F[_]] {
  def getKeys(bucketName: BucketN)(implicit client: S3): F[List[S3Key]]
}

object GetKeys {
  implicit object IOGetKeys extends GetKeys[IO] {
    def getKeys(bucketName: BucketN)(implicit client: S3): IO[List[S3Key]] = {
      val bucket = IO(client
        .bucket(bucketName.n.value)
        .map(_.keys)
        .map(_.toList)
        .map(_.map(a => S3Key(a))))
      bucket.flatMap {
        case Some(keys) => IO(keys)
        case None => IO.raiseError(new Throwable("Bucket does not exist"))
      }
    }
  }
  implicit class GetKeysOps(underlying: BucketN) {
    import com.gardenShare.gardenshare.Storage.S3.Clients._
    def keys[F[_]: GetKeys](implicit getKeys:GetKeys[F]) = getKeys.getKeys(underlying)
  }
}
