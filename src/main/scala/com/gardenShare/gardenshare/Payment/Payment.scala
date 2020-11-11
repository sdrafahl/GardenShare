package com.gardenShare.gardenshare.Payment

import cats.effect.IO
import com.gardenShare.gardenshare.Concurrency.Concurrency._
import java.time.{LocalDate, OffsetDateTime}
import org.mdedetrich.stripe.Config._
import org.mdedetrich.stripe.v1.Charges.{ChargeInput, SourceInput}
import org.mdedetrich.stripe.v1.Charges.Source.MaskedCard
import org.mdedetrich.stripe.v1.Charges.SourceInput.Customer
import org.mdedetrich.stripe.v1.Tokens.TokenData
import org.mdedetrich.stripe.v1.Tokens.TokenInput
import org.mdedetrich.stripe.v1.Tokens
import org.mdedetrich.stripe.v1.Tokens._
import akka.http.scaladsl.DefaultSSLContextCreation
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.actor.ClassicActorSystemProvider
import akka.actor.ActorSystem
import scala.util.Success
import scala.util.Failure
import org.mdedetrich.stripe.v1.Charges
import org.mdedetrich.stripe.v1.Currency
import com.gardenShare.gardenshare.domain.Payment._
import akka.http.scaladsl.HttpExt
import com.gardenShare.gardenshare.Config.GetStripePrivateKey
import org.mdedetrich.stripe.v1.Charges.Charge

abstract class MakePayment[F[_]] {
  def makePayment(c: Payment)(implicit a: ActorSystem, h: HttpExt): F[Charge]
}

object MakePayment {
  implicit object IOMakePayment extends MakePayment[IO] {
    def makePayment(c: Payment)(implicit a: ActorSystem, h: HttpExt): IO[Charge] = {
      val cardData = TokenData.Card(c.c.expMonth, c.c.expYear, c.c.cardNumber).copy(cvc = Option(c.c.cvc))
      val tokenInput = TokenInput(cardData)
      IO.fromFuture(IO(Tokens.create(tokenInput)())).flatMap{
        case Failure(err) => IO.raiseError(err)
        case Success(tok) => IO(tok)
      }.map{tkn =>
        val chgsInput = Charges.ChargeInput(
              amount = c.amount,
              Currency.`United States Dollar`,
              capture = true,
              source = Some(SourceInput.Token(tkn.id))
        )
        IO.fromFuture(IO(Charges.create(chgsInput)())).flatMap{
          case Failure(err) => IO.raiseError(err)
          case Success(tok) => IO(tok)
        }
      }.flatMap(a => a)
    }
  }
  implicit class MakePaymentOps(underlying: Payment) {
    def pay[F[_]: MakePayment](implicit mkp: MakePayment[F], a: ActorSystem, h: HttpExt) = mkp.makePayment(underlying)
  }
}

object Implicits {
  implicit val system = ActorSystem.create()
  implicit val http = Http()
}
