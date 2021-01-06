package com.gardenShare.gardenshare.web

import org.scalajs.dom.document
import org.scalajs.dom
import dom.document
import scalatags.JsDom.all._
import cats.effect.IO
import org.scalajs.dom.raw.Element
import scalatags.JsDom.TypedTag
import scalatags.stylesheet._
import scalacss.DevDefaults._
import com.gardenShare.gardenshare.web.Default._
import scalacss.internal.LengthUnit.px
import cats.effect.Sync
import org.scalajs.dom.html.Div

abstract class Modal[F[_]: Sync] {
  def createModal: F[TypedTag[Div]]
}

object Modal {
  def apply[F[_]: Sync](content: TypedTag[Div], givenId: String, isOpen: () => Boolean) = new Modal[F] {
    def createModal: F[TypedTag[Div]] = {
      val displayDisplay = isOpen() match {
        case true => "block"
        case false => "none"
      }      
      Sync[F].delay(div(
        cls := "modal",
        display := displayDisplay,
        id := givenId,
        div(
          cls := "modal-content",
          span(cls := "close", "&times"),
          content
        )
      ))
    }
  }
}
