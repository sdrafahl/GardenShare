package com.gardenShare.gardenshare

import com.gardenShare.gardenshare.Email
import cats.effect.IO
import com.gardenShare.gardenshare.CogitoClient
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.PaymentCommandEvaluator.PaymentCommandEvaluatorOps
import java.net.URL
import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext

abstract class ApplyUserToBecomeSeller[F[_]] {
  def applyUser(userName: Email, address: Address,refreshUrl: URL, returnUrl: URL)(implicit ec: ExecutionContext): F[ApplyUserToBecomeSellerResponse]
}

object ApplyUserToBecomeSeller {
  def apply[F[_]: ApplyUserToBecomeSeller] = implicitly[ApplyUserToBecomeSeller[F]]

  implicit def createIOApplyUserToBecomeSeller(
    implicit gupn: GetUserPoolId[IO],
    cognito: CogitoClient[IO],
    g:GetStore[IO],
    insertSlickEmailRef: InsertAccountEmailReference[IO],
    paymentCommandEvaluator: PaymentCommandEvaluator[IO]) = new ApplyUserToBecomeSeller[IO] {
    def applyUser(userName: Email, address: Address, refreshUrl: URL, returnUrl: URL)(implicit ec: ExecutionContext): IO[ApplyUserToBecomeSellerResponse] = {      
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
            link <- CreateStripeAccountLink(account.getId(), refreshUrl, returnUrl).evaluate
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
