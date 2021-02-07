package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.UserEntities.Email
import cats.effect.IO
import com.gardenShare.gardenshare.domain.User.UserInfo
import cats.effect.ContextShift
import com.gardenShare.gardenshare.domain.Store.Store

abstract class AddProductToStoreForSeller[F[_]] {
  def add(s: Email, produce: Produce, am:Amount)(implicit g: GetUserInfo[F], a:AddProductToStore[F], cs: ContextShift[F]): F[Unit]
}

object AddProductToStoreForSeller {
  implicit object IOAddProductToStoreForSeller extends AddProductToStoreForSeller[IO] {
    def add(s: Email, produce: Produce, am:Amount)(implicit g: GetUserInfo[IO],a:AddProductToStore[IO], cs: ContextShift[IO]): IO[Unit] = {
      g.getInfo(s).flatMap{(ui: UserInfo) =>
        ui.store match {
          case Some(store) => {
            a.add(store, produce, am)
          }
          case None => IO.raiseError(new Throwable("User is not a seller or does not have a store associated."))
        }        
      }      
    }
  }
}
