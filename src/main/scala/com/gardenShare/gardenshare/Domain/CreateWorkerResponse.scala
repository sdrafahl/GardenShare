package com.gardenShare.gardenshare

import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder

sealed abstract class CreateWorkerResponse

object CreateWorkerResponse {
  case object WorkerCreatedSuccessfully extends CreateWorkerResponse
  sealed case class WorkerFailedToCreate(msg: String) extends CreateWorkerResponse

  private[this] val createWorkerResponseDecoder: Decoder[CreateWorkerResponse] = Decoder.decodeString.emap{
    case "WorkerCreatedSuccessfully" => Right(WorkerCreatedSuccessfully)
    case a                          =>  Right(WorkerFailedToCreate(a))
  }

  private[this] val createWorkerResponseEncoder: Encoder[CreateWorkerResponse] = Encoder.encodeString.contramap{
    case WorkerCreatedSuccessfully => "WorkerCreatedSuccessfully"
    case WorkerFailedToCreate(a)   => a
  }

  implicit lazy final val createStoreResponseCodec: Codec[CreateWorkerResponse] = Codec.from(createWorkerResponseDecoder, createWorkerResponseEncoder)
}

