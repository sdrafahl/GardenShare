package com.gardenShare.gardenshare.Sequence
import scala.util.Either._
import scala.util.Either
import scala.util.Try

abstract class SequenceEithers {
  def sequenceList[B, C](lsE: List[Either[B, C]]): Either[B, List[C]]
}

object SequenceEithers {
  implicit def apply() = default
  implicit object default extends SequenceEithers {
    def sequenceList[B, C](lsE: List[Either[B, C]]): Either[B, List[C]] = {
      val badOnes = lsE.filter(_.isLeft)
      badOnes.isEmpty match {
        case false => Left(badOnes.collect{ case Left(err) => err}.head)
        case true => Right(badOnes.collect{ case Right(a) => a})
      }
    }
  }
  implicit class Ops[B, C](underlying: List[Either[B, C]]) {
    def sequence(implicit seq: SequenceEithers) = seq.sequenceList(underlying)
  }
}
