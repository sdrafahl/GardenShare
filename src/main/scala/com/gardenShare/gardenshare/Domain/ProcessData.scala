package com.gardenShare.gardenshare

case class ProcessData[F[_], D, E, G](data: F[D], op: D => E, errOp: Throwable => G)
