package com.gardenShare.gardenshare.web

import org.scalajs.dom.document
import org.scalajs.dom
import dom.document
import scalatags.JsDom.all._
import cats.effect.IO

object MainJVM {
  def run(args: Array[String]): IO[Unit] = IO{
     val btn = button(
            "Click me",
            onclick := { () =>
                dom.window.alert("Hello, world this is in Scala")
        })

        // intentional overkill to demonstrate scalatags
        val content =
            div(cls := "foo",
                div(cls := "bar",
                    h2("Hello"),
                    btn
                )
            )

    val root = dom.document.getElementById("root")
    root.innerHTML = ""
    root.appendChild(content.render)
  }
}
