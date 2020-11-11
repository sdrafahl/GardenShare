package com.gardenShare.gardenshare.Storage.S3

import com.gardenShare.gardenshare.domain.S3._
import fs2.Stream
import cats.effect._
import software.amazon.awssdk.services.s3.S3Client
import fs2.aws.s3._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.Or
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.numeric.Greater
import eu.timepit.refined._
import eu.timepit.refined.refineV
import eu.timepit.refined.auto._

abstract class GetS3Stream[F[_]] {
  def getStreamFromS3(a: GetStreamFromS3)(implicit client: S3Client, blocker: Blocker, cs: ContextShift[F]): LazyStream[F, Byte]  
}

object GetS3Stream {
  def apply[F[_]: GetS3Stream]() = implicitly[GetS3Stream[F]]
  implicit object IOGetS3Stream extends GetS3Stream[IO] {
    def getStreamFromS3(a: GetStreamFromS3)(implicit client: S3Client, blocker: Blocker, cs: ContextShift[IO]): LazyStream[IO, Byte] = {      
      LazyStream(S3.create[IO](client, blocker).flatMap { s3 =>        
        IO(s3.readFileMultipart(BucketName(a.name.n), FileKey(a.key.k), 5))
      })      
    }
  }
  implicit class GetS3StreamOps(underlying: GetStreamFromS3) {
    import com.gardenShare.gardenshare.Storage.S3.Clients._
    def getStream[F[_]: GetS3Stream](implicit getStream: GetS3Stream[F], blocker: Blocker, cs: ContextShift[F]) = getStream.getStreamFromS3(underlying)
  }
}
