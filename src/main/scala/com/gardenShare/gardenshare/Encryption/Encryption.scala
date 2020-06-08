package com.gardenShare.gardenshare.Encryption

import com.gardenShare.gardenshare.Config.PrivateKey
import javax.crypto.Cipher
import org.apache.commons.codec.binary.Base64
import com.gardenShare.gardenshare.Config.EncryptionAlgorithm
import com.gardenShare.gardenshare.Config.RSA
import java.security.KeyFactory
import com.gardenShare.gardenshare.Config.PubKey
import java.io.BufferedReader
import java.io.FileReader
import java.security.spec.X509EncodedKeySpec
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.net.URI
import jakarta.xml.bind.DatatypeConverter

abstract class Encryption {
  def encrypt(textToEncrypt: String, alg:EncryptionAlgorithm, publicKey: PubKey): Array[Byte]
}

object Encryption {
  Security.addProvider(new BouncyCastleProvider());
  def apply() = DefaultEncryption
  implicit object DefaultEncryption extends Encryption {
    def encrypt(textToEncrypt: String, alg:EncryptionAlgorithm, publicKey: PubKey) = {
      val (cipherDecrypter) = alg match {
        case RSA => Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC")
      }
      cipherDecrypter.init(Cipher.ENCRYPT_MODE, publicKey.underlying)
      val dataAsBytes = Base64.decodeBase64(textToEncrypt)
      
      cipherDecrypter.doFinal(dataAsBytes)
    }
  }

  def getKey(fileName: String): String = {
    val bufferedReader = new BufferedReader(new FileReader(fileName))
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

  def getPublicKey(key: String): PubKey = {
    val keyWithoutHeaders = key
      .replace("-----BEGIN PUBLIC KEY-----\n", "")
      .replace("-----END PUBLIC KEY-----", "")
    val encoded = Base64.decodeBase64(keyWithoutHeaders)
    val keyFactory = KeyFactory.getInstance("RSA")
    PubKey(keyFactory.generatePublic(new X509EncodedKeySpec(encoded)).asInstanceOf[RSAPublicKey])
  }
}

