package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Email
import cats.effect.IO
import com.gardenShare.gardenshare.UserInfo

abstract class AddProductToStoreForSeller[F[_]] {
  def add(s: Email, produce: Produce, am:Amount)(implicit g: GetUserInfo[F], a:AddProductToStore[F]): F[Unit]
}

object AddProductToStoreForSeller {
  implicit object IOAddProductToStoreForSeller extends AddProductToStoreForSeller[IO] {
    def add(s: Email, produce: Produce, am:Amount)(implicit g: GetUserInfo[IO],a:AddProductToStore[IO]): IO[Unit] = {      
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
