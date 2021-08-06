package com.gardenShare.gardenshare

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import io.circe.Codec
import eu.timepit.refined.api.RefType
import io.circe.Decoder
import io.circe.Encoder

sealed case class Rating(score: Rating.RatingScore, message: String)

object Rating {
  type RatingScore = Int Refined LessEqual[10]

  private[this] def parseRatingScore(s: Int): Either[String, RatingScore] = RefType.applyRef[RatingScore](s)

  private[this] lazy val ratingScoreDecoder: Decoder[RatingScore] = Decoder.decodeInt.emap(parseRatingScore)
  private[this] lazy val ratingScoreEncoder: Encoder[RatingScore] = Encoder.encodeInt.contramap(_.value)
  implicit lazy val ratingScoreCodec: Codec[RatingScore] = Codec.from(ratingScoreDecoder, ratingScoreEncoder)
}
