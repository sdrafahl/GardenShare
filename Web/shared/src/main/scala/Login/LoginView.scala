package com.gardenShare.gardenshare.web

import org.scalajs.dom.document
import org.scalajs.dom
import dom.document
import scalatags.JsDom.all._
import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.Functor
import cats.implicits._
import cats.effect.Sync
import scalatags.JsDom.TypedTag
import org.scalajs.dom.html.Div

abstract class LoginView[F[_]: Sync] {
  def createLogin(r: Ref[F, Boolean]): F[TypedTag[Div]]
}

object LoginView {
  implicit object IOLoginView extends LoginView[IO] {
    def createLogin(r: Ref[IO, Boolean]): IO[TypedTag[Div]] = {
      val innerLoginModal = div(
        input(`type` := "text", id := "userName", name := "userName"),
        br(),
        input(`type` := "password", id := "password", name := "password"),
        br()
      )
       r
        .get
        .flatMap{(isVisible) =>
          Modal[IO](
            innerLoginModal,
            "loginModal",
            () => isVisible
          ).createModal
        }
    }
  }
}
