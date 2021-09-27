package com.gardenShare.gardenshare

import cats.effect.IO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile
import scala.language.implicitConversions

abstract class InsertOrderItIntoBuyerCompleteTable[F[_]] {
  def completeOrder(orderid: OrderId)(implicit cs: ContextShift[F]): F[Unit]
}

object InsertOrderItIntoBuyerCompleteTable {
  implicit def createIOinsertOrderItIntoBuyerCompleteTable(client: PostgresProfile.backend.DatabaseDef) = new InsertOrderItIntoBuyerCompleteTable[IO] {
    def completeOrder(orderid: OrderId): IO[Unit] = {
      val table = BuyerOrderCompleteTable.buyerOrderCompleteTable
      val baseQuery = BuyerOrderCompleteTable.buyerOrderCompleteTable.returning(table)
      val query = (baseQuery += orderid).transactionally
      IO.fromFuture(IO(client.run(query))).map(_ => ())
    }
  }
}
