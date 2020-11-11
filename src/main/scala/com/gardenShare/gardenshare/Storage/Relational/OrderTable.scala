package com.gardenShare.gardenshare.Storage.Relational

import slick.jdbc.PostgresProfile.api._
import slick.dbio.DBIOAction
import slick.lifted.AbstractTable
import cats.effect._
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import scala.util.Success
import scala.util.Failure

object OrderTable {
  class OrderTable(tag: Tag) extends Table[(Int, String)](tag, "orders") {
    def orderId = column[Int]("orderId", O.PrimaryKey, O.AutoInc)
    def orderState = column[String]("orderState")
    def * = (orderId, orderState)
  }
  val orders = TableQuery[OrderTable]
}

abstract class OrderState
case class OrderInit() extends OrderState
case class Paid() extends OrderState
case class Completed() extends OrderState
case class BadState() extends OrderState
case class Order(orderId: Int, orderState: OrderState)

abstract class GetOrdersByProductId[F[_]] {
  def getProductsByOrderId(orderId: Int): F[Option[Order]]
}

object GetOrdersByProductId {
  def parseState(s: String): OrderState = {
    s match {
      case "OrderInit" => OrderInit()
      case "paid" => Paid()
      case "completed" => Completed()
      case _ => BadState()
    }
  }


  def apply[F[_]: GetOrdersByProductId]() = implicitly[GetOrdersByProductId[F]]
  implicit object IOGetOrdersByProductId extends GetOrdersByProductId[IO] {
    def getProductsByOrderId(orderId: Int): IO[Option[Order]] = {
      val query = for {
        orders <- OrderTable.orders if orders.orderId equals orderId
      } yield (orders.orderId, orders.orderState)
      IO.fromFuture(IO(Setup.db.run(query.result)))
        .map(_.toList)
        .map(_.map(a => Order(a._1, parseState(a._2))))
        .map(_.headOption)
    }
  }
}

abstract class CreateOrderInDB[F[_]] {
  def createOrder(state: OrderState): F[Order]
}

object CreateOrderInDB {
  def orderToString(state: OrderState): String = {
    state match {
      case OrderInit() => "OrderInit"
      case Paid() => "paid"
      case Completed() => "completed"
      case _ => "bad state"
    }
  }
  implicit object IOCreateOrder extends CreateOrderInDB[IO] {
    def createOrder(state: OrderState): IO[Order] = {
      val query = OrderTable.orders
      val qu = OrderTable.orders.returning(query)
      val res = qu ++= List((0, orderToString(state)))
      IO.fromFuture(IO(Setup.db.run(res)))
        .map(_.toList)
        .map { a =>
          val resp = a.head
          Order(resp._1, GetOrdersByProductId.parseState(resp._2))
        }
    }
  }
}

abstract class ChangeOrderState[F[_]] {
  def changeState(orderId: Int, st: OrderState): F[Order]
}

object ChangeOrderState {
  implicit object IOChangeOrderState extends ChangeOrderState[IO] {
    def changeState(orderId: Int, st: OrderState): IO[Order] = {      
      val query = OrderTable
        .orders
        .filter(_.orderId === orderId)
        .map(or => (or.orderId, or.orderState))
        .update((orderId, CreateOrderInDB.orderToString(st)))
        .asTry

      IO.fromFuture(IO(Setup.db.run(query)))
        .map(c => c.map(_ => Order(orderId, st)))
        .flatMap {
          case Success(order) => IO(order)
          case Failure(err) => IO.raiseError(err)
        }
    }
  }
}
