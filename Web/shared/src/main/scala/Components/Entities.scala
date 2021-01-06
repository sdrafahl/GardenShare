package com.gardenShare.gardenshare.web

import org.scalajs.dom.document
import org.scalajs.dom
import scalatags.JsDom.TypedTag
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Element

case class Component[+A <: Element](e: TypedTag[A])
