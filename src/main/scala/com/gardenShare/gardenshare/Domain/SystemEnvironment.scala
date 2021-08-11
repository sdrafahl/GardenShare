package com.gardenShare.gardenshare

abstract class SystemEnvionment

object SystemEnvionment {
  case object Testing extends SystemEnvionment
  case object Production extends SystemEnvionment
}
