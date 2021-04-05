package com.gardenShare.gardenshare

import io.circe.Encoder
import io.circe.Decoder
import io.circe.syntax._
import ShapesDerivation._

object ParsingDecodingImplicits {
  implicit val zoneDateTimeParser = ZoneDateTimeParsingEncoding.ZoneDateTimeParser
  implicit val produceParser = Produce.ProduceParser
  implicit val produceEncode = Produce.ProduceEncodeToString
  implicit val stateParser = State.StateParser
  implicit val stateEncoder = State.StateEncoder
  implicit val userTypeParser = UserType.UserTypeParser
  implicit val userTypeEncoder = UserType.UserTypeEncoder
  implicit val currencyParser = Currency.CurrencyParser
  implicit val currencyEncoder = Currency.CurrencyEncoder
  implicit val priceUnitParser = PriceUnit.PriceUnitParser
  implicit val priceUnitEncoder = PriceUnit.PriceUnitStringEncode
  implicit val urlParser = URLParsingDecoding.URLParser
  implicit val urlEncoder = URLParsingDecoding.URLEncoder
  implicit val emailParser = Email.EmailParser
  implicit val emailEncoder = Email.EmailEncoder

  implicit def createEncoder[A](implicit en: EncodeToString[A]): Encoder[A] = Encoder.instance {
    case a => en.encode(a).asJson
  }

  implicit def createDecoder[A](implicit parser: Parser[A]) = Decoder.decodeString.emap(s => parser.parse(s))

}
