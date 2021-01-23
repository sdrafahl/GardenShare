package com.gardenShare.gardenshare.Encryption

import com.gardenShare.gardenshare.Config.PrivateKey
import javax.crypto.Cipher
import org.apache.commons.codec.binary.Base64
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
  def decrypt(encryptedText: Array[Byte], privateKey: PrivateKey): String
}

object Decrypt {
  Security.addProvider(new BouncyCastleProvider());
  implicit def apply() = DefaultDecryptor
  implicit object DefaultDecryptor extends Decrypt {
    def decrypt(encryptedText: Array[Byte], privateKey: PrivateKey) = {
      val cipherDecrypter = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC")
      cipherDecrypter.init(Cipher.DECRYPT_MODE, privateKey.underlying)
      new String(Base64.encodeBase64(cipherDecrypter.doFinal(encryptedText)))
    }
  }  
}

