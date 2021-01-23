package com.gardenShare.gardenshare.GetProductDescription

import com.gardenShare.gardenshare.domain.Products.ProductDescription
import com.gardenShare.gardenshare.Config.GetDescriptionBucketName
import com.gardenShare.gardenshare.ParseDescription.ParseDescriptionStream
import com.gardenShare.gardenshare.Storage.S3.GetS3Stream
import cats.effect.IO
import com.gardenShare.gardenshare.domain.Entities._
import com.gardenShare.gardenshare.domain.S3.GetStreamFromS3
import com.gardenShare.gardenshare.domain.S3.BucketK
import com.gardenShare.gardenshare.Storage.S3.GetS3Stream.GetS3StreamOps
import cats.effect.ContextShift
import cats.effect.Blocker
import com.gardenShare.gardenshare.ParseDescription.ParseDescriptionStream.ParseDescriptionStreamOp
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import cats.FlatMap
import cats.implicits._
import cats.effect.Async

abstract class GetproductDescription[F[_]] {
  def getProdDesc(c: GetproductDescriptionCommand)(implicit parser: ParseDescriptionStream[F], cs: ContextShift[F], blocker: Blocker): F[List[ProductDescription]]
}

object GetproductDescription {
  def apply[F[_]: GetproductDescription]() = implicitly[GetproductDescription[F]]

  implicit def getproductDescription[F[_]: FlatMap: Async](implicit getStream:GetS3Stream[F], getDesBuck: GetDescriptionBucketName[F], getTypeSafeConfig: GetTypeSafeConfig[F]) = new GetproductDescription[F] {
    def getProdDesc(c: GetproductDescriptionCommand)(implicit parser: ParseDescriptionStream[F], cs: ContextShift[F], blocker: Blocker): F[List[ProductDescription]] = {
      for {
        desBucket <- getDesBuck.get
        commandToGetStream = GetStreamFromS3(desBucket, BucketK(c.key))
        stream = commandToGetStream.getStream
        parsedStream = stream.parse
        streamOfDescs <- parsedStream.s
        listOfDesc <- streamOfDescs.compile.toList
      } yield listOfDesc
    }
  }

  implicit class GetproductDescriptionOps(underlying: GetproductDescriptionCommand) {
    def getDesc[F[_]:GetproductDescription](implicit getDesc: GetproductDescription[F], parser :ParseDescriptionStream[F], cs: ContextShift[F], blocker: Blocker) = getDesc.getProdDesc(underlying)
  }  
}
