package com.gardenShare.gardenshare

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._

case class Rating(score: Rating.RatingScore, message: String)

object Rating {
  type RatingScore = Int Refined LessEqual[10]
}
