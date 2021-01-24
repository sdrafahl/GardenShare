package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.domain.User._
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import cats.effect.IO
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.Config.GetUserPoolName
import com.gardenShare.gardenshare.Config.GetUserPoolId
import scala.jdk.CollectionConverters._
import com.gardenShare.gardenshare.UserEntities.Sellers
import com.gardenShare.gardenshare.UserEntities.Requester

abstract class GetUserInfo[F[_]] {
  def getInfo(userName: Email): F[UserInfo]
}

object GetUserInfo {
  implicit def createIOGetUserInfo(implicit cognito: CogitoClient[IO], gupn: GetUserPoolId[IO]) = new GetUserInfo[IO] {
    def getInfo(userName: Email): IO[UserInfo] = {
      gupn.exec().flatMap{userPoolId =>
        cognito.listGroupsForUser(userName.underlying, userPoolId).map{resp =>
          val groups = resp
            .groups()
            .asScala
            .toList
            .map(_.groupName())

          val userType = if(groups.contains("Sellers")) {
            Sellers
          } else {
            Requester
          }
          UserInfo(userName, userType)
        }
      }
    }
  }
  implicit class GetUserInfoOps(underlying: Email) {
    def getUserInfo[F[_]: GetUserInfo] = implicitly[GetUserInfo[F]].getInfo(underlying)
  }
}
