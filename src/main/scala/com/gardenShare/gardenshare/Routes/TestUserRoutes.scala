
package com.gardenShare.gardenshare

import org.http4s.HttpRoutes
import cats.effect.Async
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.GetUserPoolId
import org.http4s.dsl.Http4sDsl
import com.gardenShare.gardenshare.GetUserPoolName
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.GetTypeSafeConfig
import cats.FlatMap
import cats.implicits._
import io.circe.generic.auto._, io.circe.syntax._
import com.gardenShare.gardenshare.Password
import com.gardenShare.gardenshare.DeleteStore
import cats.effect.ContextShift

/**
Please do not use in production
  */
object TestUserRoutes {
  def userRoutes[F[_]: Async: GetUserPoolId: CogitoClient: GetTypeSafeConfig: FlatMap:GetUserPoolName:DeleteStore:ContextShift]()
      : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case DELETE -> Root / "user" / "delete" / email => {
        emailParser.parse(email) match {
          case Left(_) => Ok(ResponseBody("email is not valid", false).asJson.toString())
          case Right(email) => {            
            val pgm = for {
              id <- implicitly[GetUserPoolId[F]].exec()
              resp <- implicitly[CogitoClient[F]].adminDeleteUser(email ,id)
              _ <- implicitly[DeleteStore[F]].delete(email)
            } yield resp
            pgm.flatMap(re => Ok(ResponseBody(re.toString(), true).asJson.toString()))
          }
        }        
      }
      case POST -> Root / "user" / email / password => {
        emailParser.parse(email) match {
          case Left(_) => Ok(ResponseBody("email is not valid", false).asJson.toString())
          case Right(ema) => {
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
  }
}
