package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.domain.Products.ProductDescription
import cats.effect.IO
import com.gardenShare.gardenshare.domain.Products.S3DescriptionAddress
import com.gardenShare.gardenshare.Storage.S3.ReadS3File
import com.gardenShare.gardenshare.Storage.S3.ReadS3File._
import awscala._, s3._
import awscala.s3.S3
import software.amazon.awssdk.services.s3.S3Client
import com.gardenShare.gardenshare.Storage.S3.Clients._
import com.gardenShare.gardenshare.domain.Products.ParseDescriptionAddress
import com.gardenShare.gardenshare.domain.Products.ParseDescriptionAddress._
import com.gardenShare.gardenshare.domain.Products.DescriptionAddress
import com.gardenShare.gardenshare.domain.Products.ParsedDescription
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

abstract class GetDescription[F[_]] {
  def getDescription(t: S3DescriptionAddress)(implicit readSeFile: ReadS3File[F]): F[ProductDescription]
}

object GetDescription {
  def apply[F[_]: GetDescription]() = implicitly[GetDescription[F]]
  implicit object IOS3GetDescription extends GetDescription[IO] {
    def getDescription(t: S3DescriptionAddress)(implicit readSeFile: ReadS3File[IO]): IO[ProductDescription] = {
      readSeFile
        .readFromS3(t.bucketName, t.path)
        .map{b =>
          parser.decode[ProductDescription](b)
        }.flatMap {          
            case Right(proDes) => IO(proDes)
            case Left(err) => IO.raiseError(new Throwable(s"Error: ${err.getMessage()}"))
        }
    }
  }
  implicit class Ops(underlying: S3DescriptionAddress) {
    def getDesc[F[_]:GetDescription: ReadS3File](implicit g: GetDescription[F]) = g.getDescription(underlying)
  }
}


