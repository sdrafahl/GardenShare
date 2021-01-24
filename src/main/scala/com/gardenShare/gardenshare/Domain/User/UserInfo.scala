package com.gardenShare.gardenshare.domain.User

import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.UserEntities.UserType

case class UserInfo(email: Email, userType: UserType)
