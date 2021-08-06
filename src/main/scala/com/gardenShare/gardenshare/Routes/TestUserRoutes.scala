
package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import cats.effect.Async
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.GetUserPoolId
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.GetTypeSafeConfig
import cats.implicits._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.Password
import com.gardenShare.gardenshare.DeleteStore
import cats.effect.ContextShift

/**
Please do not use in production
  */
object TestUserRoutes {
  def userRoutes[F[_]:
      Async:
      GetUserPoolId:
      CogitoClient:
      GetTypeSafeConfig:
      DeleteStore:
      GetUserPoolName:
      ContextShift
  ](): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case DELETE -> Root / "user" / "delete" / Email(email) => {
        val pgm = for {
           id <- implicitly[GetUserPoolId[F]].exec()
           resp <- implicitly[CogitoClient[F]].adminDeleteUser(email ,id)
           _ <- implicitly[DeleteStore[F]].delete(email)
         } yield resp
        pgm.flatMap(re => Ok(ResponseBody(re.toString(), true).asJson.toString()))
      }
      case POST -> Root / "user" / Email(email) / Password(password) => {
        val pgm = for {
          id <- implicitly[GetUserPoolId[F]].exec()
          poolName <- implicitly[GetUserPoolName[F]].exec()
          res <- implicitly[CogitoClient[F]].adminCreateUser(email, password, id, poolName.name)              
        } yield res
        pgm.flatMap(re => Ok(ResponseBody(re.toString(), true).asJson.toString()))
      }
    }
  }
}
