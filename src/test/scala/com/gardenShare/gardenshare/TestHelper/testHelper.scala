package com.gardenShare.gardenshare

import com.typesafe.config.ConfigFactory
import org.http4s.Request
import cats.effect.IO
import org.http4s.Uri
import org.http4s._
import org.http4s.implicits._
import eu.timepit.refined.auto._
import fs2.text
import io.circe.fs2._
import io.circe.syntax._
import java.time.ZonedDateTime
import PaymentCommandEvaluator._
import com.stripe.model.Account
import cats.implicits._
import org.http4s.circe.CirceEntityCodec._

object UserTestsHelper {
  lazy implicit val config = ConfigFactory.load()
  lazy implicit val dbClient = PostGresSetup.createPostgresClient
  val executionStuff = ConcurrencyHelper.createConcurrencyValues(2)
  implicit val cs = executionStuff._3
  implicit val ec = executionStuff._2
  implicit val timer = executionStuff._5

  /**
    Do Not Use in production
    */
  def deleteUserAdmin(email: Email) = {
    val uriToDeleteUser =
      Uri.fromString(s"/user/delete/${email.underlying.value}").toOption.get
    val requestToDelteUser = Request[IO](Method.DELETE, uriToDeleteUser)

    TestUserRoutes
      .userRoutes[IO]()
      .orNotFound(requestToDelteUser)
      .attempt
      .unsafeRunSync()
  }

  def createUser(email: Email, password: String) = {
    val registrationArgs = s"${email.underlying.value}/${password}"

    val uriArg =
      Uri.fromString(s"/user/signup/$registrationArgs").toOption.get

    val regTestReq = Request[IO](Method.POST, uriArg)

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(regTestReq)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, UserCreationRespose])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def adminCreateUser(email: Email, password: String) = {
    val registrationArgs = s"${email.underlying.value}/${password}"

    val uriArg =
      Uri.fromString(s"/user/$registrationArgs").toOption.get

    val regTestReq = Request[IO](Method.POST, uriArg)

    TestUserRoutes
      .userRoutes[IO]()
      .orNotFound(regTestReq)
      .unsafeRunSync()
  }

  def authUser(email: Email, password: String) = {
    val registrationArgs = s"${email.underlying.value}/${password}"

    val uriArg =
      Uri.fromString(s"/user/auth/$registrationArgs").toOption.get

    val regTestReq = Request[IO](Method.GET, uriArg)

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(regTestReq)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, AuthUserResponse])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def authToken(jwtToken: String) = {
    val uriArg = Uri.fromString(s"/user/jwt/${jwtToken}").toOption.get

    val authRequest = Request[IO](Method.GET, uriArg)

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(authRequest)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, IsJwtValidResponse])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def applyUserToBecomeSeller(jwt: String, a: ApplyUserToBecomeSellerData) = {
    val uriArg = Uri.fromString(s"/user/apply-to-become-seller").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val request = Request[IO](Method.POST, uriArg, headers = headers).withEntity(a.asJson.toString())

    UserRoutes
        .userRoutes[IO]()
        .orNotFound(request)
        .unsafeRunSync()
        .as[ApplyUserToBecomeSellerResponse]
        .unsafeRunSync()
  }

  def verifyUserAsSeller(jwt: String, address: Address) = {
    val uriArg = Uri.fromString(s"/user/verify-user-as-seller").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val request = Request[IO](Method.POST, uriArg, headers = headers).withEntity(address.asJson.toString())

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, ResponseBody])
      .compile
      .toList
      .unsafeRunSync()
  }

  def getUserInfo(jwt: String): UserInfo = {
    val uriArg = Uri.fromString("/user/info").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val infoRequest = Request[IO](Method.GET, uriArg, headers = headers)

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(infoRequest)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, UserInfo])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def deletestore(email: Email)(implicit d:DeleteStore[IO]) = d.delete(email).unsafeRunSync()
  def addStore(s: CreateStoreRequest)(implicit i:InsertStore[IO]) = i.add(List(s))
  def getStore(email: Email)(implicit d:GetStore[IO]) = d.getStoresByUserEmail(email).unsafeRunSync().head

  def getStores(limit: Int, rangeInSeconds: Int, jwt: String, address: Address) = {
    val uriArg = Uri.fromString(s"/store/${limit}/${rangeInSeconds}").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    
    val storeRequest = Request[IO](Method.POST, uriArg, headers = headers).withEntity(address.asJson.toString())

    StoreRoutes
      .storeRoutes[IO]
      .orNotFound(storeRequest)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, NearestStores])
      .compile
      .toList
      .unsafeRunSync()
      .head     
  }

  def makeRequestToGetProductDescription(key: String) = {
    val uri = Uri.fromString(s"productDescription/${key}").toOption.get

    val request = Request[IO](Method.GET, uri)

    ProductDescriptionRoutes
      .productDescriptionRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[ProductDescription]
      .unsafeRunSync()
  }

  def addProductToStore(produce: String, jwt: String, am: Amount) = {

    val uri = Uri.fromString(s"product/add/${produce}/${am.quantityOfCurrency}/${am.currencyType.show}").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val request = Request[IO](Method.POST, uri, headers = headers)
    
    ProductRoutes
      .productRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .compile
      .toList
      .unsafeRunSync()
      .head

  }

  def getProductsFromStore(jwt: String) = {
    val uri = Uri.fromString(s"product").toOption.get
    val headers = Headers.of(Header("authentication", jwt))

    val request = Request[IO](Method.GET, uri, headers = headers)

    ProductRoutes
      .productRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, ListOfProduce])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def createStoreOrderRequest(jwtOfTheBuyer: String ,emailOfTheSeller: Email, s: StoreOrderRequestBody) = {
    val uri = Uri.fromString(s"storeOrderRequest/${emailOfTheSeller.underlying.value}").toOption.get
    val headers = Headers.of(Header("authentication", jwtOfTheBuyer))
    val request = Request[IO](Method.POST, uri, headers = headers).withEntity(s.asJson.toString())

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, StoreOrderRequestWithId])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def getListOfOrdersFromSellers(jwtOfSeller: String, from: ZonedDateTime, to: ZonedDateTime) = {
    val fromForURL = Base64EncoderDecoder().encode(from.toString()).get
      
    val toForURL = Base64EncoderDecoder().encode(to.toString()).get

    val uri = Uri.fromString(s"storeOrderRequest/seller/${fromForURL}/${toForURL}").toOption.get
    val headers = Headers.of(Header("authentication", jwtOfSeller))
    val request = Request[IO](Method.GET, uri, headers = headers)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, StoreOrderRequestsBelongingToSellerBody])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def getOrderStatus(orderId: Int) = {
    val uri = Uri.fromString(s"storeOrderRequest/status/${orderId}").toOption.get
    val request = Request[IO](Method.GET, uri)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, StoreOrderRequestStatusBody])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def acceptOrder(orderId:Int, jwt: String) = {
    val uri = Uri.fromString(s"storeOrderRequest/accept/${orderId}").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val request = Request[IO](Method.POST, uri, headers = headers)  

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
  }

  def denyOrder(orderId:Int, jwt: String) = {
    val uri = Uri.fromString(s"storeOrderRequest/deny/${orderId}").toOption.get
    val headers = Headers.of(Header("authentication", jwt))
    val request = Request[IO](Method.POST, uri, headers = headers)  

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
  }

  def deleteAllStoreOrdersForSeller[F[_]](e: Email) = implicitly[DeleteStoreOrderRequestsForSeller[IO]].delete(e)

  def clearSlickAccounts(emails: List[Email]) = ClearStripeAccounts(emails).evaluate[IO].unsafeRunSync()

  def insertSlickEmailReference(email: Email, slickId: String) = implicitly[InsertAccountEmailReference[IO]].insert(slickId, email)

  def deleteSlickEmailReference(email: Email) = implicitly[DeleteAccountEmailReferences[IO]].delete(email)

  def initiatePayment(buyerJwt: String, orderId: Int, receiptEmail: Email, paymentType: PaymentType) = {    
    val uri = Uri.fromString(s"storeOrderRequest/initiate-payment/${orderId}/${receiptEmail.underlying.value}/${paymentType.show}").toOption.get
    val headers = Headers.of(Header("authentication", buyerJwt))
    val request = Request[IO](Method.POST, uri, headers = headers)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, PaymentIntentToken])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def verifyPayment(orderId: Int, buyerJwt: String) = {
    val uri = Uri.fromString(s"storeOrderRequest/verify-payment/${orderId}").toOption.get
    val headers = Headers.of(Header("authentication", buyerJwt))
    val request = Request[IO](Method.POST, uri, headers = headers)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, PaymentVerification])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def createCustomAccount() = CreateCustomAccountCommand().evaluate[IO].unsafeRunSync()

  def deleteCustomAccount(account: Account) = DeleteAccountCommand(account.getId()).evaluate[IO].unsafeRunSync()

  def confirmOrder(orderId: String, card: CreditCard) = ConfirmPaymentIntentCard(orderId, card).evaluate.unsafeRunSync()

  def getStatusOfIntent(intentId: String) = GetPaymentIntentCommand(intentId).evaluate.unsafeRunSync()

  def getPaymentIntentID(orderId: Int) = implicitly[GetPaymentIntentFromStoreRequest[IO]].search(orderId.toString()).unsafeRunSync()

  def getTestStripeAccount = "acct_1IV66N2R0KHt4WIV"

}
