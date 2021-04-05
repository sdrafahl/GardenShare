package com.gardenShare.gardenshare

abstract class State
case object AL extends State
case object AK extends State
case object AZ extends State
case object AR extends State
case object CA extends State
case object CO extends State
case object CT extends State
case object DE extends State
case object DC extends State
case object FL extends State
case object GA extends State
case object HI extends State
case object ID extends State
case object IL extends State
case object IN extends State
case object IA extends State
case object KS extends State
case object KY extends State
case object LA extends State
case object ME extends State
case object MD extends State
case object MA extends State
case object MI extends State
case object MN extends State
case object MS extends State
case object MO extends State
case object MT extends State
case object NE extends State
case object NV extends State
case object NH extends State
case object NJ extends State
case object NM extends State
case object NY extends State
case object NC extends State
case object ND extends State
case object OH extends State
case object OK extends State
case object OR extends State
case object PA extends State
case object RI extends State
case object SC extends State
case object SD extends State
case object TN extends State
case object TX extends State
case object UT extends State
case object VT extends State
case object VA extends State
case object WA extends State
case object WV extends State
case object WI extends State
case object WY extends State

object State {
  implicit object StateParser extends Parser[State] {
    def parse(x: String): Either[String, State] = x match {
      case "AL" => Right(AL)
      case "AK" => Right(AK)
      case "AZ" => Right(AZ)
      case "AR" => Right(AR)
      case "CA" => Right(CA)
      case "CO" => Right(CO)
      case "CT" => Right(CT)
      case "DE" => Right(DE)
      case "DC" => Right(DC)
      case "FL" => Right(FL)
      case "GA" => Right(GA)
      case "HI" => Right(HI)
      case "ID" => Right(ID)
      case "IL" => Right(IL)
      case "IN" => Right(IN)
      case "IA" => Right(IA)
      case "KS" => Right(KS)
      case "KY" => Right(KY)
      case "LA" => Right(LA)
      case "ME" => Right(ME)
      case "MD" => Right(MD)
      case "MA" => Right(MA)
      case "MI" => Right(MI)
      case "MN" => Right(MN)
      case "MS" => Right(MS)
      case "MO" => Right(MO)
      case "MT" => Right(MT)
      case "NE" => Right(NE)
      case "NV" => Right(NV)
      case "NH" => Right(NH)
      case "NJ" => Right(NJ)
      case "NM" => Right(NM)
      case "NY" => Right(NY)
      case "NC" => Right(NC)
      case "ND" => Right(ND)
      case "OH" => Right(OH)
      case "OK" => Right(OK)
      case "OR" => Right(OR)
      case "PA" => Right(PA)
      case "RI" => Right(RI)
      case "SC" => Right(SC)
      case "SD" => Right(SD)
      case "TN" => Right(TN)
      case "TX" => Right(TX)
      case "UT" => Right(UT)
      case "VT" => Right(VT)
      case "VA" => Right(VA)
      case "WA" => Right(WA)
      case "WV" => Right(WV)
      case "WI" => Right(WI)
      case "WY" => Right(WY)
      case x    => Left(s"invalid state provided ${x}")
    }
  }

  implicit object StateEncoder extends EncodeToString[State] {
    def encode(x: State): String = x match {
      case AL => "AL"
      case AK => "AK"
      case AZ => "AZ"
      case AR => "AR"
      case CA => "CA"
      case CO => "CO"
      case CT => "CT"
      case DE => "DE"
      case DC => "DC"
      case FL => "FL"
      case GA => "GA"
      case HI => "HI"
      case ID => "ID"
      case IL => "IL"
      case IN => "IN"
      case IA => "IA"
      case KS => "KS"
      case KY => "KY"
      case LA => "LA"
      case ME => "ME"
      case MD => "MD"
      case MA => "MA"
      case MI => "MI"
      case MN => "MN"
      case MS => "MS"
      case MO => "MO"
      case MT => "MT"
      case NE => "NE"
      case NV => "NV"
      case NH => "NH"
      case NJ => "NJ"
      case NM => "NM"
      case NY => "NY"
      case NC => "NC"
      case ND => "ND"
      case OH => "OH"
      case OK => "OK"
      case OR => "OR"
      case PA => "PA"
      case RI => "RI"
      case SC => "SC"
      case SD => "SD"
      case TN => "TN"
      case TX => "TX"
      case UT => "UT"
      case VT => "VT"
      case VA => "VA"
      case WA => "WA"
      case WV => "WV"
      case WI => "WI"
      case WY => "WY"
    }
  }
}
