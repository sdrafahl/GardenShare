package com.gardenShare.gardenshare.Encryption

import com.gardenShare.gardenshare.Config.PrivateKey
import javax.crypto.Cipher
import org.apache.commons.codec.binary.Base64
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
  def encrypt(textToEncrypt: String, publicKey: PubKey): Array[Byte]
}

object Encryption {
  Security.addProvider(new BouncyCastleProvider());
  def apply() = DefaultEncryption
  implicit object DefaultEncryption extends Encryption {
    def encrypt(textToEncrypt: String, publicKey: PubKey) = {
      val (cipherDecrypter) = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC")
      cipherDecrypter.init(Cipher.ENCRYPT_MODE, publicKey.underlying)
      val dataAsBytes = Base64.decodeBase64(textToEncrypt)   
      cipherDecrypter.doFinal(dataAsBytes)
    }
  }
}

