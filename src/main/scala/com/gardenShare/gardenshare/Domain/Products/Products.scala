package com.gardenShare.gardenshare.domain.Products

import scala.util.Try

abstract class ParsedDescriptions {
  def getAddress: String
}

case class DescriptionAddress(address: String)
case class S3DescriptionAddress(address: String) extends ParsedDescriptions {
  def getAddress: String = address
}
case class BadDescriptionAddress()

abstract class ParseDescriptionAddress[A] {
  type B 
  def parseDescription(a: A): Either[BadDescriptionAddress, B] 
}

object ParseDescriptionAddress {
  implicit def apply[A: ParseDescriptionAddress]() = implicitly[ParseDescriptionAddress[A]]  
  implicit object ParseS3Description extends ParseDescriptionAddress[DescriptionAddress] {
    type B = S3DescriptionAddress
    def parseDescription(a: DescriptionAddress): Either[BadDescriptionAddress, B] = a.address.contains("s3@") match {
      case true => {
        Try(a.address.split("@", 2).tail.head)
          .map(add => S3DescriptionAddress(add))
          .toEither
          .left
          .map(a => BadDescriptionAddress())       
      }
      case false => Left(BadDescriptionAddress())
    }
  }
  implicit class Ops[A: ParseDescriptionAddress](underlying: A) {
    def parse[C](implicit parser: ParseDescriptionAddress[A] { type B = C }) : Either[BadDescriptionAddress,parser.B] = parser.parseDescription(underlying)
  }
}

case class CreateProductRequest(storeId: Int,descriptionAddresss: ParsedDescriptions)
case class Product(id: Int, storeId: Int, descriptionS3Address: ParsedDescriptions)
