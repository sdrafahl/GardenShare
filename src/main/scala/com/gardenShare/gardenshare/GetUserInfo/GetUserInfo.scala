package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.CogitoClient
import cats.effect.IO
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.GetUserPoolId
import scala.jdk.CollectionConverters._
import com.gardenShare.gardenshare.GetStore
import UserType._

abstract class GetUserInfo[F[_]] {
  def getInfo(userName: Email): F[UserInfo]
}

object GetUserInfo {
  implicit def createIOGetUserInfo(implicit cognito: CogitoClient[IO], gupn: GetUserPoolId[IO], getStore: GetStore[IO]) = new GetUserInfo[IO] {
    def getInfo(userName: Email): IO[UserInfo] = {
      gupn.exec().flatMap{userPoolId =>
        cognito.listGroupsForUser(userName.underlying.value, userPoolId).product(getStore.getStoresByUserEmail(userName)).map{resp =>
          val groups = resp._1.groups().asScala.toList.map(_.groupName())
          val UserTypeAndStore = if(groups.contains("Sellers")) {
            (Sellers, resp._2.headOption)
          } else {
            (Requester, None)
          }
          UserInfo(userName, UserTypeAndStore._1, UserTypeAndStore._2)
        }
      }
    }
  }
  implicit class GetUserInfoOps(underlying: Email) {
    def getUserInfo[F[_]: GetUserInfo] = implicitly[GetUserInfo[F]].getInfo(underlying)
  }
}
