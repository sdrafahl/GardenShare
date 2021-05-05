package com.gardenShare.gardenshare

import cats.Show

case class Address(street: String, city: String, zip: String, state: State)

object Address {
  implicit object AddressShow extends Show[Address] {
    def show(a: Address) = s"${a.street} ${a.city}, ${a.state.toString()} ${a.zip}"
  }
}
