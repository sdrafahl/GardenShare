package com.gardenShare.gardenshare.Config

import com.typesafe.config.ConfigFactory
import cats.effect.IO
import com.typesafe.config._
import cats.effect.Async
import scala.util.Try
import cats.syntax.functor._
import cats.Functor
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import collection.JavaConverters._
import java.security.interfaces.RSAPublicKey
import java.security.interfaces.RSAPrivateKey

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

abstract class GetEncryptionAlgorithmFromConfig[F[_]] {
  def get()(implicit getTypeSafeConfig: GetTypeSafeConfig[F]): F[EncryptionAlgorithm]
}

object GetEncryptionAlgorithmFromConfig {
  def apply[F[_]: GetEncryptionAlgorithmFromConfig] = implicitly[GetEncryptionAlgorithmFromConfig[F]]

  implicit object IOGetEncryptionAlgorithmFromConfig extends GetEncryptionAlgorithmFromConfig[IO] {
    def get()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]) = {
      for {
        conf <- getTypeSafeConfig.get("encryption.algorithm")
      } yield conf match {
        case "RSA" => RSA
      }
    }
  }
}

abstract class EncryptionAlgorithm
case object RSA extends EncryptionAlgorithm

case class PubKey(underlying: RSAPublicKey)
abstract class GetPublicKey[F[_]: GetTypeSafeConfig] {
  def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[F]): F[PubKey]
}

object GetPublicKey {
  def apply[F[_]: GetPublicKey] = implicitly[GetPublicKey[F]]
  implicit object IOGetPublicKey extends GetPublicKey[IO] {
    def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[PubKey] = {
      for {
        conf <- getTypeSafeConfig.get("encryption.publicKey")
        keyFactory = KeyFactory.getInstance("RSA")
        encodedKey: RSAPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(conf.getBytes())).asInstanceOf[RSAPublicKey]
      } yield PubKey(encodedKey)
    }
  }
}

case class PrivateKey(underlying: RSAPrivateKey)
abstract class GetPrivateKey[F[_]: GetTypeSafeConfig] {
  def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[F]): F[PrivateKey]
}

object GetPrivateKey {
  def apply[F[_]: GetPrivateKey] = implicitly[GetPrivateKey[F]]
  implicit object IOGetPrivateKey extends GetPrivateKey[IO] {
    def exec()(implicit getTypeSafeConfig: GetTypeSafeConfig[IO]): IO[PrivateKey] = {
      for {
        conf <- getTypeSafeConfig.get("encryption.privateKey")
        encodedKey = new PKCS8EncodedKeySpec(conf.getBytes())
        keyFactory = KeyFactory.getInstance("RSA")
      } yield PrivateKey(keyFactory.generatePrivate(encodedKey).asInstanceOf[RSAPrivateKey])
    }
  }
}
