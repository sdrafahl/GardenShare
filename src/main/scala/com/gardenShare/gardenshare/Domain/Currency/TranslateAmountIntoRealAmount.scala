package com.gardenShare.gardenshare

abstract class TranslateAmountIntoRealAmount {
  def translate(a: Amount): Long
}


object TranslateAmountIntoRealAmount {
  implicit object DefaultTranslateAmountIntoRealAmount extends TranslateAmountIntoRealAmount {
    def translate(a: Amount): Long = {
      a.currencyType match {
        case USD => a.quantityOfCurrency.toLong
      }
    }
  }

  implicit class TranslateAmountIntoRealAmountOps(underlying: Amount) {
    def trans(implicit translator: TranslateAmountIntoRealAmount) = translator.translate(underlying)
  }
}
