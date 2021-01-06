package com.gardenShare.gardenshare.web

import com.gardenShare.gardenshare.web.Default._
import scalacss.defaults.Exports.StyleSheet
import scalacss.DevDefaults._

object ModalCSS extends StyleSheet.Standalone {
  import dsl._

  "modal" - (
    position.fixed,
    zIndex(1),
    paddingTop(100 px),
    left(0 px),
    top(0 px),
    width(100 %%),
    height(100 %%),
    overflow.auto
  )

  "close" - (
    color(c"#aaaaaa"),
    float.right,
    fontSize(28 px),
    cursor.pointer
  )

  "close:hover" - ()

  "close:focus" - (
    color(c"#000"),
    cursor.pointer
  )


}
