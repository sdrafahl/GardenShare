package com.gardenShare.gardenshare

sealed abstract class CreateWorkerResponse
case class WorkerCreatedSuccessfully() extends CreateWorkerResponse
case class WorkerFailedToCreate(msg: String) extends CreateWorkerResponse 
