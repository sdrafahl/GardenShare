package com.gardenShare.gardenshare.Encryption

import com.gardenShare.gardenshare.Config.PrivateKey
import javax.crypto.Cipher
import org.apache.commons.codec.binary.Base64
import com.gardenShare.gardenshare.Config.EncryptionAlgorithm
import com.gardenShare.gardenshare.Config.RSA
import java.security.KeyFactory
import java.io.File
import java.io.BufferedReader
import java.io.FileReader
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.URI
import jakarta.xml.bind.DatatypeConverter
import io.netty.handler.codec.base64.Base64Encoder


abstract class Decrypt {
  def decrypt(encryptedText: Array[Byte], alg:EncryptionAlgorithm, privateKey: PrivateKey): String
}

object Decrypt {
  Security.addProvider(new BouncyCastleProvider());
  def apply() = DefaultDecryptor
  implicit object DefaultDecryptor extends Decrypt {
    def decrypt(encryptedText: Array[Byte], alg:EncryptionAlgorithm, privateKey: PrivateKey) = {
      val cipherDecrypter = alg match {
        case RSA => Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC")
      }
      cipherDecrypter.init(Cipher.DECRYPT_MODE, privateKey.underlying)

      new String(Base64.encodeBase64(cipherDecrypter.doFinal(encryptedText)))
    }
  }

  def getPrivateKey(fileName:String) : PrivateKey = {
    val privateKeySerial = Encryption.getKey(fileName)
    PrivateKey(createPrivateKeyFromSerial(privateKeySerial))
  }

  def createPrivateKeyFromSerial(key:String):RSAPrivateKey = {
    val keyWithoutHeaders = key
      .replace("-----BEGIN PRIVATE KEY-----\n", "")
      .replace("-----END PRIVATE KEY-----", "")
    val keyEncoding = Base64.decodeBase64(keyWithoutHeaders)
    val kf = KeyFactory.getInstance("RSA")
    val keySpec = new PKCS8EncodedKeySpec(keyEncoding)
    kf.generatePrivate(keySpec).asInstanceOf[RSAPrivateKey]
  }
}

// https://stackoverflow.com/questions/11787571/how-to-read-pem-file-to-get-private-and-public-key

