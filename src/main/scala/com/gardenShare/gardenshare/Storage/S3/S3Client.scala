package com.gardenShare.gardenshare.Storage.S3

import cats.effect.IO
import software.amazon.awssdk.services.s3.S3Client
import com.gardenShare.gardenshare.Config.GetRegion
import com.gardenShare.gardenshare.Config.Region
import com.gardenShare.gardenshare.Config.GetAwsRegionFromMyRegion
import com.gardenShare.gardenshare.Config.GetAwsRegionFromMyRegion._
import cats.implicits._
import cats.Functor
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import awscala._, s3._
import fs2.{io, text, Stream}
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import scala.concurrent.ExecutionContext
import com.gardenShare.gardenshare.ParseDescription.ParseDescription.Ops
import com.gardenShare.gardenshare.ParseDescription.ParseDescription

abstract class ReadS3File[F[_]] {
  def readFromS3(bucketName: String, path: String)(implicit client: S3): F[String]
}

object Clients {
  implicit lazy val cli = S3()
}

object ReadS3File {
  implicit def apply[F[_]: ReadS3File]() = implicitly[ReadS3File[F]]
  implicit object default extends ReadS3File[IO] {
    def readFromS3(bucketName: String, path: String)(implicit client: S3): IO[String] = IO {
      client.bucket(bucketName)
        .map(bucket => bucket.getObject(path)).flatten
        .map(obj => obj.content)
        .map{str => scala.io.Source.fromInputStream(str).mkString} match {
          case None => IO.raiseError(new Throwable("Failed Reading from S3"))
          case Some(v) => IO(v)
        }
    }.flatten
  }
}
