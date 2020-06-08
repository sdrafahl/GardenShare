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
import com.gardenShare.gardenshare.Config.RSA
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

object EncryptionSpec extends TestSuite {

  val publicPem = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5szvb0+FNRGfVbwz/oYP
xR09GOBIoKHh1N5zlHCAi34AkUJ0y5fq1c9hviWA0CIOgDTS/CDKNEwiKKQAfMYd
81yct/gsmP6yKC795PbI255nnno1zOCb0kc758DvS2Vo4qFRWkgUAdkpAV/yWl6l
vugsVirjY9oYfxt7QRCqINBDKGGC57mqsgvxHgyfyN7vDMGqfj6RvIByLfxcjBB0
3wGOA3Vt3og+b+pSieRPDy+zOSGCSvUTg3Vnd4xcguEvZB+JxI0gOng38gk+85pW
dPqGrxnOM3bQKziIGg4j3M0ssTGa7wJYqxSmP7TrltaJQ3lCzqcDpPNdXUcfCMAj
tQIDAQAB
-----END PUBLIC KEY-----"""

  val tests = Tests {
    test("Encryption") {
        test("decryption") {
          test("Should decrypt some encrypted text") {            
            val publicKeyValue = Encryption.getKey("/home/sdrafahl/code/gardenshare/src/test/scala/com/gardenShare/gardenshare/Encryption/public-a.pub")
            val publicKey = Encryption.getPublicKey(publicKeyValue)
            val testText = "testText"
            val encryptor = Encryption()
            println("1.0000000000000000000000000000000")
            val encryptedText = encryptor.encrypt(testText, RSA, publicKey)
            println("2.0000000000000000000000000000000")
            val decryptor = Decrypt()
            val privateKeyValue = Decrypt.getPrivateKey("/home/sdrafahl/code/gardenshare/src/test/scala/com/gardenShare/gardenshare/Encryption/private-a.pem")
            println("3.0000000000000000000000000000000")

            val decryptedText = decryptor.decrypt(encryptedText, RSA, privateKeyValue)
            println("decryptedText !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            println(decryptedText)
            assert(testText equals decryptedText)
          }
        }
    }
  }
}
