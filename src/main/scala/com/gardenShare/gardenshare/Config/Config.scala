package com.gardenShare.gardenshare.Config

import com.typesafe.config.ConfigFactory
import cats.effect.IO
import com.typesafe.config._
import cats.effect.Async
import scala.util.Try
import cats.syntax.functor._
import cats.Functor
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import collection.JavaConverters._
import java.security.interfaces.RSAPublicKey
import java.security.interfaces.RSAPrivateKey
import java.io.BufferedReader
import java.io.FileReader
import java.io.File
import scala.io.Source
import org.apache.commons.codec.binary.Base64

abstract class GetTypeSafeConfig[F[_]:Functor] {
  def get(key: String): F[String]
}
object GetTypeSafeConfig {
  def apply[F[_]:GetTypeSafeConfig]() = implicitly[GetTypeSafeConfig[F]]

  implicit object IOGetTypeSafeConfig extends GetTypeSafeConfig[IO] {
    private lazy implicit val conf: Config = ConfigFactory.load();
    def get(key: String) =  IO(conf.getString(key))
  }
}

case class UserPoolSecret(secret: String)

abstract class GetUserPoolSecret[F[_]:Functor] {
  def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): F[UserPoolSecret]
}

object GetUserPoolSecret {
  def apply[F[_]:GetUserPoolSecret] = implicitly[GetUserPoolSecret[F]]

  implicit object IOGetUserPoolSecret extends GetUserPoolSecret[IO] {
    def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[UserPoolSecret] = for {
      conf <- getTypeSafeConfig.get("users.secret")
    } yield UserPoolSecret(conf)
  }
}

case class UserPoolName(name: String)

abstract class GetUserPoolName[F[_]: GetTypeSafeConfig:Functor] {
  def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[F]): F[UserPoolName]
}

object GetUserPoolName {
  implicit def apply[F[_]: GetUserPoolName] = implicitly[GetUserPoolName[F]]
  implicit object IOGetUserPoolName extends GetUserPoolName[IO] {
    def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[UserPoolName] = {
      for {
        conf <- getTypeSafeConfig.get("users.standardUserPoolName")
      } yield UserPoolName(conf)
    }
  }
}

case class UserPoolID(id: String)

abstract class GetUserPoolId[F[_]: GetTypeSafeConfig:Functor] {
  def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[F]): F[UserPoolID]
}

object GetUserPoolId {
  implicit def apply[F[_]: GetUserPoolId] = implicitly[GetUserPoolId[F]]
  implicit object IOGetUserPoolId extends GetUserPoolId[IO] {
    def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[UserPoolID] = {
      for {
        userPoolId <- getTypeSafeConfig.get("users.id")        
      } yield UserPoolID(userPoolId)
    }
  }
}

case class PubKey(underlying: RSAPublicKey)
abstract class GetPublicKey {
  def exec(): PubKey
}

object getKeysHelpers {
  def getKey(fileName: String): String = {
    val bufferedReader = Source.fromResource(fileName).bufferedReader()
    def readUniltEnd(acc:String, bufferedReader: BufferedReader): String = {
      val maybeLine = Option(bufferedReader.readLine())
      maybeLine match {
        case None => acc
        case Some(newLine) => readUniltEnd(acc + newLine + "\n", bufferedReader)
      }
    }
    val key = readUniltEnd("", bufferedReader)
    bufferedReader.close()
    key
  }
}

object GetPublicKey {

  private def getPublicKey(key: String): PubKey = {
    val keyWithoutHeaders = key
      .replace("-----BEGIN PUBLIC KEY-----\n", "")
      .replace("-----END PUBLIC KEY-----", "")
    val encoded = Base64.decodeBase64(keyWithoutHeaders)
    val keyFactory = KeyFactory.getInstance("RSA")
    PubKey(keyFactory.generatePublic(new X509EncodedKeySpec(encoded)).asInstanceOf[RSAPublicKey])
  }

  lazy val publicKey = getPublicKey(getKeysHelpers.getKey("public.pem"))

  def apply() = DefaultGetPublicKey

  implicit object DefaultGetPublicKey extends GetPublicKey {
    def exec(): PubKey = publicKey
  }
}

case class PrivateKey(underlying: RSAPrivateKey)
abstract class GetPrivateKey {
  def exec(): PrivateKey
}

object GetPrivateKey {
  private def createPrivateKeyFromSerial(key:String):PrivateKey = {
    val keyWithoutHeaders = key
      .replace("-----BEGIN PRIVATE KEY-----\n", "")
      .replace("-----END PRIVATE KEY-----", "")
    val keyEncoding = Base64.decodeBase64(keyWithoutHeaders)
    val kf = KeyFactory.getInstance("RSA")
    val keySpec = new PKCS8EncodedKeySpec(keyEncoding)
    PrivateKey(kf.generatePrivate(keySpec).asInstanceOf[RSAPrivateKey])
  }

  lazy val privateKey = createPrivateKeyFromSerial(getKeysHelpers.getKey("private.pem"))
  
  def apply() = DefaultGetPrivateKey
  implicit object DefaultGetPrivateKey extends GetPrivateKey {
    def exec(): PrivateKey = privateKey
  }
}
