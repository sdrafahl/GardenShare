package com.gardenShare.gardenshare

import cats.effect.IO
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import cats.effect.ContextShift

abstract class InsertIntoBuyerOrderCompleteTable[F[_]] {
  def insert(order: OrderId)(implicit cs: ContextShift[F]): F[Unit]
}

object InsertIntoBuyerOrderCompleteTable {
  implicit def createIOInsertIntoBuyerOrderCompleteTable(implicit client: PostgresProfile.backend.DatabaseDef) = new InsertIntoBuyerOrderCompleteTable[IO] {
    def insert(order: OrderId)(implicit cs: ContextShift[IO]): IO[Unit] = {
      val buyerOrderCompleteTable = BuyerOrderCompleteTable.buyerOrderCompleteTable
      val requestToAddOrder = buyerOrderCompleteTable += OrderId(0)
      IO.fromFuture(IO(client.run(requestToAddOrder.transactionally))) *> (IO.unit)
    }
  }
}
