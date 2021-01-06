package com.gardenShare.gardenshare.web

import org.scalajs.dom.document
import org.scalajs.dom
import dom.document
import scalatags.JsDom.all._
import cats.effect.IO
import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node

abstract class RenderComponent {
  def renderOnChild[A <: Element](c: Component[A])(implicit e: Element): Unit
}

object RenderComponent {
  def apply(implicit r: RenderComponent) = r
  implicit object DefaultRenderComponent extends RenderComponent {
    def renderOnChild[A <: Element](c: Component[A])(implicit e: Element): Unit = e.appendChild(c.e.render)
  }
}

