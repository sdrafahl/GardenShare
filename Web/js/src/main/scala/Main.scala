package com.gardenShare.gardenshare.web

import org.scalajs.dom.document
import org.scalajs.dom
import dom.document
import scalatags.JsDom.all._
import cats.effect.IO

object Main {
  def main(args: Array[String]): Unit = MainJVM.run(args).unsafeRunSync()
}
