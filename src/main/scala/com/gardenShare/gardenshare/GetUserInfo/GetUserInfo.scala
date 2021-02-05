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
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.Storage.Relational.GetStore
import cats.effect.ContextShift

abstract class GetUserInfo[F[_]] {
  def getInfo(userName: Email)(implicit cs: ContextShift[F]): F[UserInfo]
}

object GetUserInfo {
  implicit def createIOGetUserInfo(implicit cognito: CogitoClient[IO], gupn: GetUserPoolId[IO], getStore: GetStore[IO]) = new GetUserInfo[IO] {
    def getInfo(userName: Email)(implicit cs: ContextShift[IO]): IO[UserInfo] = {
      gupn.exec().flatMap{userPoolId =>
        cognito.listGroupsForUser(userName.underlying, userPoolId).parProduct(getStore.getStoresByUserEmail(userName)).map{resp =>
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
    def getUserInfo[F[_]: GetUserInfo: ContextShift] = implicitly[GetUserInfo[F]].getInfo(underlying)
  }
}
