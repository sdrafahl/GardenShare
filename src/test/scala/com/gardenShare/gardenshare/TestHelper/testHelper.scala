package com.gardenShare.gardenshare

import com.typesafe.config.ConfigFactory
import org.http4s.Request
import cats.effect.IO
import org.http4s.Uri
import org.http4s._
import org.http4s.implicits._
import eu.timepit.refined.auto._
import java.time.ZonedDateTime
import PaymentCommandEvaluator._
import com.stripe.model.Account
import cats.implicits._
import org.http4s.circe.CirceEntityCodec._
import AuthMiddleWear._
import cats.effect.unsafe.implicits.global
import org.typelevel.ci.CIString

object UserTestsHelper {
  lazy implicit val config = ConfigFactory.load()
  lazy implicit val dbClient = PostGresSetup.createPostgresClient
  val executionStuff = ConcurrencyHelper.createConcurrencyValues(2)
  implicit val ec = executionStuff._2

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
      .as[UserCreationRespose]
      .unsafeRunSync()    
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
      .as[AuthUserResponse]
      .unsafeRunSync()    
  }

  def authToken(jwtToken: String) = {
    val uriArg = Uri.fromString(s"/user/jwt/${jwtToken}").toOption.get

    val authRequest = Request[IO](Method.GET, uriArg)

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(authRequest)
      .unsafeRunSync()
      .as[IsJwtValidResponse]
      .unsafeRunSync()
  }

  def applyUserToBecomeSeller(jwt: String, a: ApplyUserToBecomeSellerData) = {
    val uriArg = Uri.fromString(s"/user/apply-to-become-seller").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))
    val request = Request[IO](Method.POST, uriArg, headers = headers).withEntity(a)

    UserRoutes
        .userRoutes[IO]()
        .orNotFound(request)
        .unsafeRunSync()
        .as[ApplyUserToBecomeSellerResponse]
        .unsafeRunSync()
  }

  def verifyUserAsSeller(jwt: String, address: Address) = {
    val uriArg = Uri.fromString(s"/user/verify-user-as-seller").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))
    val request = Request[IO](Method.POST, uriArg, headers = headers).withEntity(address)

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(request)
      .unsafeRunSync()
      .as[ResponseBody]
      .unsafeRunSync()
  }

  def getUserInfo(jwt: String): UserInfo = {
    val uriArg = Uri.fromString("/user/info").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))
    val infoRequest = Request[IO](Method.GET, uriArg, headers = headers)

    UserRoutes
      .userRoutes[IO]()
      .orNotFound(infoRequest)
      .unsafeRunSync()
      .as[UserInfo]
      .unsafeRunSync()
  }

  def deletestore(email: Email)(implicit d:DeleteStore[IO]) = d.delete(email).unsafeRunSync()
  def addStore(s: CreateStoreRequest)(implicit i:InsertStore[IO]) = i.add(List(s))
  def getStore(email: Email)(implicit d:GetStore[IO]) = d.getStoresByUserEmail(email).unsafeRunSync().head

  def getStores(limit: Int, rangeInMiles: Int, jwt: String, address: Address) = {
    val uriArg = Uri.fromString(s"/store/${limit}/${rangeInMiles}").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))
    
    val storeRequest = Request[IO](Method.POST, uriArg, headers = headers).withEntity(address)

    StoreRoutes
      .storeRoutes[IO]
      .orNotFound(storeRequest)
      .unsafeRunSync()
      .as[NearestStores]
      .unsafeRunSync()
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

    val uri = Uri.fromString(s"product/add/${produce}/${am.quantityOfCurrency.value}/${am.currencyType.show}").toOption.get
    
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))
    val request = Request[IO](Method.POST, uri, headers = headers)
    
    ProductRoutes
      .productRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[ResponseBody]
      .unsafeRunSync()
  }

  def getProductsFromStore(jwt: String) = {
    val uri = Uri.fromString(s"product").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))

    val request = Request[IO](Method.GET, uri, headers = headers)
    ProductRoutes
      .productRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[ListOfProduce]
      .unsafeRunSync()
  }

  def createStoreOrderRequest(jwtOfTheBuyer: String ,emailOfTheSeller: Email, s: StoreOrderRequestBody) = {
    val uri = Uri.fromString(s"storeOrderRequest/${emailOfTheSeller.underlying.value}").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwtOfTheBuyer))
    val request = Request[IO](Method.POST, uri, headers = headers).withEntity(s)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[StoreOrderRequestWithId]
      .unsafeRunSync()
  }

  def getListOfOrdersFromSellers(jwtOfSeller: String, from: ZonedDateTime, to: ZonedDateTime) = {
    val fromForURL = Base64EncoderDecoder().encode(from.toString()).get      
    val toForURL = Base64EncoderDecoder().encode(to.toString()).get

    val uri = Uri.fromString(s"storeOrderRequest/seller/${fromForURL}/${toForURL}").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwtOfSeller))
    val request = Request[IO](Method.GET, uri, headers = headers)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[StoreOrderRequestsBelongingToSellerBody]
      .unsafeRunSync()
  }

  def getOrderStatus(orderId: OrderId) = {
    val uri = Uri.fromString(s"storeOrderRequest/status/${orderId.id}").toOption.get
    val request = Request[IO](Method.GET, uri)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[StoreOrderRequestStatusBody]
      .unsafeRunSync()
  }

  def acceptOrder(orderId:OrderId, jwt: String) = {
    val uri = Uri.fromString(s"storeOrderRequest/accept/${orderId.id}").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))
    val request = Request[IO](Method.POST, uri, headers = headers)  

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
  }

  def denyOrder(orderId:Int, jwt: String) = {
    val uri = Uri.fromString(s"storeOrderRequest/deny/${orderId}").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), jwt))
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

  def initiatePayment(buyerJwt: String, orderId: OrderId, receiptEmail: Email, paymentType: PaymentType) = {    
    val uri = Uri.fromString(s"storeOrderRequest/initiate-payment/${orderId.id}/${receiptEmail.underlying.value}/${paymentType.show}").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), buyerJwt))
    val request = Request[IO](Method.POST, uri, headers = headers)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[PaymentIntentToken]
      .unsafeRunSync()
  }

  def verifyPayment(orderId: OrderId, buyerJwt: String) = {
    val uri = Uri.fromString(s"storeOrderRequest/verify-payment/${orderId.id}").toOption.get
    val headers = Headers(Header.Raw(CIString("authentication"), buyerJwt))
    val request = Request[IO](Method.POST, uri, headers = headers)

    StoreOrderRoutes
      .storeOrderRoutes[IO]
      .orNotFound(request)
      .unsafeRunSync()
      .as[PaymentVerification]
      .unsafeRunSync()
  }

  def createCustomAccount() = CreateCustomAccountCommand().evaluate[IO].unsafeRunSync()

  def deleteCustomAccount(account: Account) = DeleteAccountCommand(account.getId()).evaluate[IO].unsafeRunSync()

  def confirmOrder(orderId: String, card: CreditCard) = ConfirmPaymentIntentCard(orderId, card).evaluate.unsafeRunSync()

  def getStatusOfIntent(intentId: String) = GetPaymentIntentCommand(intentId).evaluate.unsafeRunSync()

  def getPaymentIntentID(orderId: OrderId) = implicitly[GetPaymentIntentFromStoreRequest[IO]].search(orderId).unsafeRunSync()

  def getTestStripeAccount = "acct_1IV66N2R0KHt4WIV"
}
