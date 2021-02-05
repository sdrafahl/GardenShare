package com.gardenShare.gardenshare.domain.User

import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.UserType
import com.gardenShare.gardenshare.domain.Store._

case class UserInfo(email: Email, userType: UserType, store: Option[Store])

