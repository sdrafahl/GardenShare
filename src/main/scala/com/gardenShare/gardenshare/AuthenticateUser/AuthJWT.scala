package com.gardenShare.gardenshare

import cats.effect.IO
import org.jose4j.jwk.HttpsJwks
import com.gardenShare.gardenshare.StringReps._
import com.gardenShare.gardenshare.StringReps.UseastOneRep
import com.gardenShare.gardenshare.StringReps
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwt.consumer.JwtConsumer
import com.gardenShare.gardenshare.GetUserPoolId
import cats.MonadError
import cats.implicits._

abstract class AuthJWT[F[_]] {
  def authJWT(jwt:JWTValidationTokens): F[JWTValidationResult]
}

object AuthJWT {
  implicit def apply[F[_]: AuthJWT]() = implicitly[AuthJWT[F]]

  implicit def createAuthJWT[F[_]](implicit getUserPoolId: GetUserPoolId[F], builder: HttpsJwksBuilder[F], getRegion: GetRegion[F],joseProcessJwt:JoseProcessJwt[F], getUserPoolName: GetUserPoolName[F], get: GetTypeSafeConfig[F], ae: MonadError[F, Throwable]) = new AuthJWT[F] {
    def authJWT(jwt:JWTValidationTokens): F[JWTValidationResult] = {
      (for {
        id <- getUserPoolId.exec()
        region <- getRegion.exec
        userPoolName <- getUserPoolName.exec()
        st = StringReps[USEastOne]()
        url = s"https://cognito-idp.${region.stringRep}.amazonaws.com/${id.id}/.well-known/jwks.json"
        consumer <- builder.build(url, userPoolName)
        result = consumer.process(jwt.idToken)
        jwt <- joseProcessJwt.processJwt(consumer, jwt)
      } yield jwt)
        .attempt
        .map{
          case Left(err) => InvalidToken(err.getMessage())
          case Right(res) => res
        }
    }
  }

  implicit class AuthJwtOps(underlying:JWTValidationTokens) {
    def auth[F[_]](implicit auth: AuthJWT[F], joe: JoseProcessJwt[F]) = auth.authJWT(underlying)
  }
}


abstract class JoseProcessJwt[F[_]] {
  def processJwt(c: JwtConsumer, jwt:JWTValidationTokens): F[JWTValidationResult]
}

object JoseProcessJwt {
  implicit def apply[F[_]](implicit x:JoseProcessJwt[F]) = x
  implicit def createJoseProcessJwt(implicit parseEmail: com.gardenShare.gardenshare.Parser[Email]) = new JoseProcessJwt[IO] {
    def processJwt(c: JwtConsumer, jwt:JWTValidationTokens): IO[JWTValidationResult] = {
      IO(c.processToClaims(jwt.idToken))
        .attempt
        .map{
          case Left(err) => InvalidToken("Invalid token")
          case Right(tokn) => {
            parseEmail.parse(tokn.getClaimValueAsString("email")) match {                
                case Right(email) => ValidToken(Some(email))
                case Left(_) => InvalidToken("Invalid email")
              }
          }
        }
    }
  }
}

abstract class HttpsJwksBuilder[F[_]] {
  def build(url: String, userPoolId: UserPoolName): F[JwtConsumer]
}

object HttpsJwksBuilder {
  implicit def apply[F[_]: HttpsJwksBuilder]() = implicitly[HttpsJwksBuilder[F]]
  implicit object default extends HttpsJwksBuilder[IO] {
    def build(url: String, userPoolId: UserPoolName): IO[JwtConsumer] = {
      IO {
        val httpsjwks = new HttpsJwks(url)
        val resolver = new HttpsJwksVerificationKeyResolver(httpsjwks)
        new JwtConsumerBuilder()
          .setVerificationKeyResolver(resolver)
          .setExpectedAudience(userPoolId.name)
          .build()
      }      
    }
  }
}
