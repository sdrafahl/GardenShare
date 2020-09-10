package com.gardenShare.gardenshare.domain.Products

import scala.util.Try

case class DescriptionAddress(address: String)
case class S3DescriptionAddress(bucketName: String , path:String)
case class ParsedDescription(s3Desc: Option[S3DescriptionAddress])
case class BadDescriptionAddress()

abstract class ParseDescriptionAddress[A] {
  def parseDescription(a: A): Either[BadDescriptionAddress, ParsedDescription] 
}

object ParseDescriptionAddress {
  implicit def apply[A: ParseDescriptionAddress]() = implicitly[ParseDescriptionAddress[A]]  
  implicit object ParseS3Description extends ParseDescriptionAddress[DescriptionAddress] {
    def parseDescription(a: DescriptionAddress): Either[BadDescriptionAddress, ParsedDescription] = a.address.contains("s3@") match {
      case true => {
        val segments = a.address.split("@")
        Try((segments.tail.head, segments.tail.tail.head))
          .map(add => ParsedDescription(Option(S3DescriptionAddress(add._1, add._2))))
          .toEither
          .left
          .map(a => BadDescriptionAddress())
      }
      case false => Left(BadDescriptionAddress())
    }
  }
  implicit class Ops[A: ParseDescriptionAddress](underlying: A) {
    def parse[C](implicit parser: ParseDescriptionAddress[A]) : Either[BadDescriptionAddress, ParsedDescription] = parser.parseDescription(underlying)
  }
}

case class CreateProductRequest(storeId: Int,descriptionAddresss: DescriptionAddress)
case class Product(id: Int, storeId: Int, descriptionS3Address: DescriptionAddress)
