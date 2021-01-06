package com.gardenShare.gardenshare.web

import scalacss.DevDefaults._
import scalacss.ProdDefaults._

package object Default {
  implicit val CssSettings = scalacss.devOrProdDefaults
  import CssSettings._
}


