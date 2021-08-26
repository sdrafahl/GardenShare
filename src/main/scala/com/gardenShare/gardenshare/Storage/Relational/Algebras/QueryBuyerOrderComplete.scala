package com.gardenShare.gardenshare

import cats.effect.IO
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import cats.effect.ContextShift

abstract class QueryBuyerOrderComplete[F[_]] {
  def search(id: OrderId)(implicit cs: ContextShift[F]): F[Option[OrderId]]
}

object QueryBuyerOrderComplete {
  implicit def createIOQueryBuyerOrderComplete(implicit client: PostgresProfile.backend.DatabaseDef) = new QueryBuyerOrderComplete[IO] {
    def search(id: OrderId)(implicit cs: ContextShift[IO]): IO[Option[OrderId]] = {
      val query = for {
        response <- BuyerOrderCompleteTable.buyerOrderCompleteTable if response.order === id
      } yield response
      IO.fromFuture(IO(client.run(query.result))).map(_.headOption)
    }
  }
}
