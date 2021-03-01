package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities.AuthenticatedUser
import com.gardenShare.gardenshare.domain.Store.Store

case class ResponseBody(msg: String, success: Boolean)
case class ListOfProduce(listOfProduce: List[ProductWithId])
case class UserCreationRespose(msg: String, userCreated: Boolean)
case class AuthUserResponse(
  msg: String,
  auth: Option[AuthenticatedUser],
  authenticated: Boolean
)
case class IsJwtValidResponse(msg: String, valid: Boolean)  

case class NoJWTTokenProvided()
case class NearestStores(store: List[RelativeDistanceAndStore])
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

abstract class RoutesTypes
case class TestingAndProductionRoutes() extends RoutesTypes
case class OnlyProductionRoutes() extends RoutesTypes


