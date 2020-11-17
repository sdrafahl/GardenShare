package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities.AuthenticatedUser
import com.gardenShare.gardenshare.UserEntities.Group
import com.gardenShare.gardenshare.domain.Store.Store

case class ResponseBody(msg: String)
case class UserCreationRespose(msg: String, userCreated: Boolean)
case class AuthUserResponse(
  msg: String,
  auth: Option[AuthenticatedUser],
  authenticated: Boolean
)
case class IsJwtValidResponse(
  msg: String,
  valid: Boolean,
      groups: List[Group]
  )  

case class NoJWTTokenProvided()
case class StoresAdded(store: List[Store])
case class FailedToAddStore(msg: String)
case class FailedToFindStore(msg: String)
case class InvalidLimitProvided(msg: String)
case class InvalidRangeProvided(msg: String)
case class ListOfStores(l: List[Store])

case class InvalidDescriptionName()
case class InvalidStoreIDInput()

case class CantFindDescriptionBucketName(msg: String)
case class NoEmail()
case class InvalidProductId(msg: String)
