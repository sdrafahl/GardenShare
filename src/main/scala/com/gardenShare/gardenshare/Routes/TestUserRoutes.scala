
package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import cats.effect.Async
import com.gardenShare.gardenshare.Config.GetUserPoolSecret
import com.gardenShare.gardenshare.Storage.Users.Cognito.CogitoClient
import com.gardenShare.gardenshare.Config.GetUserPoolId
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.Config.GetUserPoolName
import cats.effect.IO
import com.gardenShare.gardenshare.UserEntities.Email
import com.gardenShare.gardenshare.Config.GetTypeSafeConfig
import cats.FlatMap
import cats.implicits._
import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.UserEntities.Password
import com.gardenShare.gardenshare.Storage.Relational.DeleteStore

/**
Please do not use in production
  */
object TestUserRoutes {
  def userRoutes[F[_]: Async: GetUserPoolId: CogitoClient: GetTypeSafeConfig: FlatMap:GetUserPoolName:DeleteStore]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case DELETE -> Root / "user" / "delete" / email => {
        val pgm = for {
          id <- implicitly[GetUserPoolId[F]].exec()
          resp <- implicitly[CogitoClient[F]].adminDeleteUser(Email(email) ,id)
          _ <- implicitly[DeleteStore[F]].delete(Email(email))
        } yield resp
        pgm.flatMap(re => Ok(ResponseBody(re.toString(), true).asJson.toString()))
      }
      case POST -> Root / "user" / email / password => {
        val ema = Email(email)
        val pass = Password(password)
        val pgm = for {
          id <- implicitly[GetUserPoolId[F]].exec()
          poolName <- implicitly[GetUserPoolName[F]].exec()
          res <- implicitly[CogitoClient[F]].adminCreateUser(ema, pass, id, poolName.name)
        } yield res
        pgm.flatMap(re => Ok(ResponseBody(re.toString(), true).asJson.toString()))
      }
    }
  }
}
