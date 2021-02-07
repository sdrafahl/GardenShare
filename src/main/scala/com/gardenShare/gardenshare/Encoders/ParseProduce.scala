package com.gardenShare.gardenshare

abstract class ParseProduce[T] {
  def parse(t:T): Either[String ,Produce]
}

object ParseProduce {
  implicit object StringParseProduce extends ParseProduce[String] {
    def parse(t:String): Either[String ,Produce] = {
      t match {
        case "Brown-Oyster-Mushrooms" => Right(BrownOysterMushrooms)
        case "BrownOysterMushrooms" => Right(BrownOysterMushrooms)
        case _ => {
          Left("Invalid produce")
        }
      }
    }
  }
  implicit class ParseProduceOps[T](underlying: T) {
    def parseProduce(implicit p:ParseProduce[T]) = p.parse(underlying)
  }
}
