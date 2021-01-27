package com.gardenShare.gardenshare.Encoders

import io.circe.Encoder, io.circe.syntax._
import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.circe.KeyEncoder
import com.gardenShare.gardenshare.Storage.Relational.Order
import com.gardenShare.gardenshare.Storage.Relational.CreateOrderInDB._
import com.gardenShare.gardenshare.Storage.Relational.OrderState
import com.gardenShare.gardenshare.UserEntities._
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.generic.JsonCodec, io.circe.syntax._
import com.gardenShare.gardenshare.domain._
import cats.syntax.functor._
import io.circe.{ Decoder, Encoder }, io.circe.generic.auto._
import io.circe.syntax._
import com.gardenShare.gardenshare.UserEntities.Requester
import cats.syntax.functor._
import io.circe.{ Decoder, Encoder }, io.circe.generic.auto._
import io.circe.syntax._
import com.gardenShare.gardenshare.domain.Store._

object Encoders {
  implicit val orderEncoder: Encoder[OrderState] = new Encoder[OrderState] {
    final def apply(a: OrderState): Json = Json.fromString(orderToString(a))
  }

  implicit val stateEncoder: Encoder[State] = new Encoder[State] {
    final def apply(a: State): Json = a match {
    case AL => "AL".asJson
    case AK => "AK".asJson
    case AZ => "AZ".asJson
    case AR => "AR".asJson
    case CA => "CA".asJson
    case CO => "CO".asJson
    case CT => "CT".asJson
    case DE => "DE".asJson
    case DC => "DC".asJson
    case FL => "FL".asJson
    case GA => "GA".asJson
    case HI => "HI".asJson
    case ID => "ID".asJson
    case IL => "IL".asJson
    case IN => "IN".asJson
    case IA => "IA".asJson
    case KS => "KS".asJson
    case KY => "KY".asJson
    case LA => "LA".asJson
    case ME => "ME".asJson
    case MD => "MD".asJson
    case MA => "MA".asJson
    case MI => "MI".asJson
    case MN => "MN".asJson
    case MS => "MS".asJson
    case MO => "MO".asJson
    case MT => "MT".asJson
    case NE => "NE".asJson
    case NV => "NV".asJson
    case NH => "NH".asJson
    case NJ => "NJ".asJson
    case NM => "NM".asJson
    case NY => "NY".asJson
    case NC => "NC".asJson
    case ND => "ND".asJson
    case OH => "OH".asJson
    case OK => "OK".asJson
    case OR => "OR".asJson
    case PA => "PA".asJson
    case RI => "RI".asJson
    case SC => "SC".asJson
    case SD => "SD".asJson
    case TN => "TN".asJson
    case TX => "TX".asJson
    case UT => "UT".asJson
    case VT => "VT".asJson
    case VA => "VA".asJson
    case WA => "WA".asJson
    case WV => "WV".asJson
    case WI => "WI".asJson
    case WY => "WY".asJson
    }
  }

  implicit val UserTypeEncoder: Encoder[UserType] = Encoder.instance {
    case Requester => "Requester".asJson
    case Sellers => "Sellers".asJson
    case InvalidType => "InvalidType".asJson
  }

  implicit object CreateWorkerResponseEncoder extends Encoder[CreateWorkerResponse] {
    override def apply(ut: CreateWorkerResponse) = ut match {
      case WorkerCreatedSuccessfully() => WorkerCreatedSuccessfully().asJson
      case WorkerFailedToCreate(msg) => WorkerFailedToCreate(msg).asJson
    }
  }

  implicit object SellerResponseEncoder extends Encoder[SellerResponse] {
    override def apply(sr: SellerResponse) = sr match {
      case SellerRequestSuccessful() => SellerRequestSuccessful().asJson
      case SellerRequestFailed(msg) => SellerRequestFailed(msg).asJson
    }
  }

  implicit val sellerResponseDecoder = List[Decoder[SellerResponse]](
    Decoder[SellerRequestSuccessful].widen,
    Decoder[SellerRequestFailed].widen
  ).reduceLeft(_ or _)

  implicit val CreateWorkerResponseDecoder = List[Decoder[CreateWorkerResponse]](
    Decoder[WorkerCreatedSuccessfully].widen,
    Decoder[WorkerFailedToCreate].widen
  ).reduceLeft(_ or _)

  implicit val userTypeDecoder: Decoder[UserType] = Decoder.decodeString.emap{
    case "Requester" => Right(Requester)
    case "Sellers" => Right(Sellers)
    case _ => Right(InvalidType)
  }

  implicit val stateDecoder: Decoder[State] = Decoder.decodeString.emap{(s: String) => s.map(_.toUpper) match {
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
    case _ => Left("invalid state provided")
  }


  }
}
