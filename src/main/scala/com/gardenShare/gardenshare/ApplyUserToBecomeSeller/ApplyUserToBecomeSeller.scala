package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Email
import cats.effect.IO
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.UserType
import com.gardenShare.gardenshare.Sellers
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.InsertStore
import com.gardenShare.gardenshare.CreateStoreRequest
import com.gardenShare.gardenshare.PaymentCommandEvaluator.PaymentCommandEvaluatorOps
import java.net.URL
import java.net.URI
import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import cats.effect.ContextShift
import com.gardenShare.gardenshare.Encoders._
import scala.concurrent.ExecutionContext

case class ApplyUserToBecomeSellerResponse(url: URL)

abstract class ApplyUserToBecomeSeller[F[_]] {
  def applyUser(userName: Email, address: Address,refreshUrl: URL, returnUrl: URL)(implicit cs: ContextShift[F], ec: ExecutionContext): F[ApplyUserToBecomeSellerResponse]
}

object ApplyUserToBecomeSeller {
  def apply[F[_]: ApplyUserToBecomeSeller] = implicitly[ApplyUserToBecomeSeller[F]]

  implicit def createIOApplyUserToBecomeSeller(
    implicit gupn: GetUserPoolId[IO],
    cognito: CogitoClient[IO],
    g:GetStore[IO],
    insertStore:InsertStore[IO],
    insertSlickEmailRef: InsertAccountEmailReference[IO],
    paymentCommandEvaluator: PaymentCommandEvaluator[IO]) = new ApplyUserToBecomeSeller[IO] {
    def applyUser(userName: Email, address: Address, refreshUrl: URL, returnUrl: URL)(implicit cs: ContextShift[IO], ec: ExecutionContext): IO[ApplyUserToBecomeSellerResponse] = {      
      for {
        stores <- g.getStoresByUserEmail(userName)
        _ <- stores match {
          case List() => IO.pure("")
          case _ => IO.raiseError(new Throwable("Store already exists at that address"))
        }
        userPoolId <- gupn.exec()
        groupsResponse <- cognito.listGroupsForUser(userName.underlying.value, userPoolId)
        isAlreadyASeller = groupsResponse.groups().asScala.find(gt => gt.groupName() == "Sellers")
        res <- isAlreadyASeller match {
          case Some(_) => IO.raiseError(new Throwable("User is already a seller"))
          case None => for {
            account <- CreateStripeConnectedAccount(userName).evaluate
            _ <- insertSlickEmailRef.insert(account.getId(), userName)
            _ = println("About to link")
            link <- CreateStripeAccountLink(account.getId(), refreshUrl, returnUrl).evaluate
            _ = println("created the link")
          } yield link.getUrl()
        }
        parsedUrl <- Try(new URL(res)) match {
	  case Success(url) => IO.pure(url)
          case Failure(err) => IO.raiseError(new Throwable(s"URL is not parsable: ${err.getMessage()}"))
        }
      } yield ApplyUserToBecomeSellerResponse(parsedUrl)
    }
  }
}
