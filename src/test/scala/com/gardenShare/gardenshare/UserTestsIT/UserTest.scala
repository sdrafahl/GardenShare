package com.gardenShare.gardenshare

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite
import com.gardenShare.gardenshare.Email
import com.gardenShare.gardenshare.Password
import fs2.text
import io.circe.fs2._
import io.circe.syntax._
import utest.TestSuite
import utest.test
import utest.Tests
import com.gardenShare.gardenshare.UserInfo
import com.gardenShare.gardenshare.Sellers
import com.gardenShare.gardenshare.Shows._
import com.gardenShare.gardenshare.DeleteStore
import com.gardenShare.gardenshare.GetStore
import com.gardenShare.gardenshare.Address
import com.gardenShare.gardenshare.IA
import com.gardenShare.gardenshare.Store._
import com.gardenShare.gardenshare.InsertStore
import com.gardenShare.gardenshare.DeleteStoreOrderRequestsForSeller
import cats.effect.ContextShift
import scala.util.Try
import java.time.ZonedDateTime
import com.gardenShare.gardenshare.SearchStoreOrderRequestTable
import com.gardenShare.gardenshare.SearchAcceptedStoreOrderRequestTableByID._
import com.gardenShare.gardenshare.SearchDeniedStoreOrderRequestTable._
import com.gardenShare.gardenshare.SearchDeniedStoreOrderRequestTable
import com.typesafe.config.ConfigFactory
import com.gardenShare.gardenshare.PostGresSetup
import com.gardenShare.gardenshare.ConcurrencyHelper
import java.net.URL
import com.gardenShare.gardenshare.PaymentCommandEvaluator.PaymentCommandEvaluatorOps
import io.circe.generic.auto._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.api.RefType
import PaymentVerificationStatus._
import com.stripe.model.Account
import ParsingDecodingImplicits._

object UserTestSpec extends TestSuite {  

  import UserTestsHelper._

  val tests = Tests {
    test("User Routes") {

      val testEmail = Email("shanedrafahl@gmail.com")
      val testPassword = "teST12$5jljasdf"
      val testRefreshURL = new URL("http://localhost:3000/")
      val testReturnURL = new URL("http://localhost:3000/")
      test("/user/signup/shanedrafahl@gmail.com/teST12$5jljasdf") {
        test("Should register a user") {
          UserTestsHelper.deleteUserAdmin(testEmail)

          val responseFromCreatingUser = UserTestsHelper.createUser(testEmail, testPassword)
          val expectedUserCreatedResponse = UserCreationRespose(
            "User Request Made: CodeDeliveryDetailsType(Destination=s***@g***.com, DeliveryMedium=EMAIL, AttributeName=email)",
            true
          )
          assert(responseFromCreatingUser equals expectedUserCreatedResponse)
        }
      }
      test("/user/auth/shanedrafahl@gmail.com/teST12$5jljasdf") {
        test("Should authenticate a valid user") {
          UserTestsHelper.deleteUserAdmin(testEmail)
          UserTestsHelper.adminCreateUser(testEmail, testPassword)
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          assert(r.auth.isDefined)
          assert(r.msg equals "jwt token is valid")
        }
      }
      test("/user/jwt/") {
        test("Should authenticate a value JWT token") {
          UserTestsHelper.deleteUserAdmin(testEmail)
          UserTestsHelper.adminCreateUser(testEmail, testPassword)
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          val jwtToken = r.auth.get.jwt
          val authResponse = UserTestsHelper.authToken(jwtToken)
          assert(authResponse.msg equals "Token is valid")
        }
      }
      test("/user/apply-to-become-seller") {
        test("Should create application to become a seller"){
          UserTestsHelper.clearSlickAccounts(List(testEmail))
          UserTestsHelper.deleteUserAdmin(testEmail)
          UserTestsHelper.deletestore(testEmail)
          UserTestsHelper.adminCreateUser(testEmail, testPassword)          
          val r = UserTestsHelper.authUser(testEmail, testPassword)
          val jwtToken = r.auth.get.jwt
          val address = Address("500 hickman Rd", "Waukee", "50263", IA)
          val responseForApplication = UserTestsHelper.applyUserToBecomeSeller(jwtToken,ApplyUserToBecomeSellerData(address,testRefreshURL, testReturnURL ))
          val userVerified = UserTestsHelper.verifyUserAsSeller(jwtToken, address)
          assert(!responseForApplication.head.url.toString().isEmpty())          
        }
      }
      test("/user/verify-user-as-seller") {
        test("Should create application to become a seller") {

          val testEmailSlickAccount = Email("gardensharetest@gmail.com")
          val testPass = "testPass12$"
          val accountID = "acct_1IaV882R5fpwMLQe"

          UserTestsHelper.deleteUserAdmin(testEmailSlickAccount)
          UserTestsHelper.deletestore(testEmailSlickAccount)
          UserTestsHelper.deleteSlickEmailReference(testEmailSlickAccount).unsafeRunSync()

          UserTestsHelper.adminCreateUser(testEmailSlickAccount, testPass)
          UserTestsHelper.insertSlickEmailReference(testEmailSlickAccount, accountID).unsafeRunSync()

          val r = UserTestsHelper.authUser(testEmailSlickAccount, testPass)
          val jwtToken = r.auth.get.jwt
          val address = Address("501 hickman Rd", "Waukee", "50263", IA)

          val response = UserTestsHelper.verifyUserAsSeller(jwtToken, address).head
          val expectedResponse = ResponseBody("User is now a seller and address was set", true)
          assert(response.equals(expectedResponse))

          val info = UserTestsHelper.getUserInfo(jwtToken)
          val expectedInfo = UserInfo(testEmailSlickAccount, Sellers, Some(Store(info.store.get.id, address, testEmailSlickAccount)))
          assert(info equals expectedInfo)
          val store = UserTestsHelper.getStore(testEmailSlickAccount)
          assert(store.address equals address)
        }
      }
    }
  }
 }

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
        .body
        .through(text.utf8Decode)
        .through(stringArrayParser)
        .through(decoder[IO, ApplyUserToBecomeSellerResponse])
        .compile
        .toList
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
      .body
      .through(text.utf8Decode)
      .through(stringArrayParser)
      .through(decoder[IO, ProductDescription])
      .compile
      .toList
      .unsafeRunSync()
      .head
  }

  def addProductToStore(produce: String, jwt: String, am: Amount) = {

    val currencyEncoder = implicitly[EncodeToString[Currency]]

    val uri = Uri.fromString(s"product/add/${produce}/${am.quantityOfCurrency}/${currencyEncoder.encode(am.currencyType)}").toOption.get
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

  def deleteAllStoreOrdersForSeller[F[_]: DeleteStoreOrderRequestsForSeller: ContextShift](e: Email) = implicitly[DeleteStoreOrderRequestsForSeller[IO]].delete(e)

  def clearSlickAccounts(emails: List[Email]) = ClearStripeAccounts(emails).evaluate[IO].unsafeRunSync()

  def insertSlickEmailReference(email: Email, slickId: String) = implicitly[InsertAccountEmailReference[IO]].insert(slickId, email)

  def deleteSlickEmailReference(email: Email) = implicitly[DeleteAccountEmailReferences[IO]].delete(email)

  def initiatePayment(buyerJwt: String, orderId: Int, receiptEmail: Email, paymentType: PaymentType) = {    
    val uri = Uri.fromString(s"storeOrderRequest/initiate-payment/${orderId}/${receiptEmail.underlying.value}/${implicitly[EncodeToString[PaymentType]].encode(paymentType)}").toOption.get
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

}
