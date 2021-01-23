package com.gardenShare.gardenshare.GetGarden

import utest._
import cats.effect.IO
import com.gardenShare.gardenshare.CreateGarden.CreateGarden
import com.gardenShare.gardenshare.Storage.Relational.InsertGarden
import com.gardenShare.gardenshare.Storage.Relational.Gardens._
import com.gardenShare.gardenshare.Storage.Relational.Plants._
import com.gardenShare.gardenshare.Storage.Relational.InsertPlant
import com.gardenShare.gardenshare.GardenData.Garden
import com.gardenShare.gardenshare.GardenData.Plant
import com.gardenShare.gardenshare.CreateGarden.CreateGarden._
import org.mockito.internal.handler.MockHandlerFactory
import com.gardenShare.gardenshare.Storage.Relational.GetGardenFromDatabase
import com.gardenShare.gardenshare.Storage.Relational.GetPlant
import com.gardenShare.gardenshare.Encryption.Encryption
import com.gardenShare.gardenshare.Config.PubKey
import java.security.spec.X509EncodedKeySpec
import com.gardenShare.gardenshare.Encryption.Decrypt
import com.gardenShare.gardenshare.Config.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import jakarta.xml.bind.DatatypeConverter
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Paths
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory
import java.io.FileInputStream
import java.security.Security
import com.gardenShare.gardenshare.Config.GetPublicKey
import com.gardenShare.gardenshare.Config.GetPrivateKey

object EncryptionSpec extends TestSuite {

  val tests = Tests {
    test("Encryption") {
        test("decryption") {
          test("Should decrypt some encrypted text") {
            val pubKey = GetPublicKey().exec()
            val privateKey = GetPrivateKey().exec()
            val testText = "testText"
            val encryptor = Encryption()
            val encryptedText = encryptor.encrypt(testText, pubKey)
            val decryptor = Decrypt()
            val decryptedText = decryptor.decrypt(encryptedText, privateKey)
            assert(testText equals decryptedText)
          }
        }
    }
  }
}
