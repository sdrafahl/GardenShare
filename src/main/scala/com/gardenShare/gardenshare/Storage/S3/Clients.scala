package com.gardenShare.gardenshare.Storage.S3

import awscala._, s3._
import software.amazon.awssdk.services.s3.S3Client

object Clients {
  implicit val s3 = S3()
  implicit val s3Client = S3Client.builder().build()
}
