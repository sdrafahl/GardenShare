package com.gardenShare.gardenshare

final case class Password(underlying: String)

object Password {
  def unapply(passStr: String): Option[Password] = Some(Password(passStr))
}
