package com.gardenShare.gardenshare

abstract class IsWithinRange {
  def isInRange(range: DistanceInMiles, dist: DistanceInMiles): Boolean
}

object IsWithinRange {
  def apply() = default
  implicit object default extends IsWithinRange {
    def isInRange(range: DistanceInMiles, dist: DistanceInMiles): Boolean = dist.distance < range.distance
  }
  implicit class Ops(underlying: DistanceInMiles) {
    def inRange(range: DistanceInMiles)(implicit isWith: IsWithinRange) = isWith.isInRange(range, underlying)
  }
}
