package com.gardenShare.gardenshare

abstract class Parser[A] {
  def parse(x:String): Either[String, A]
}

