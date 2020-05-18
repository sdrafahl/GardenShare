package com.gardenShare.gardenshare.Config

import utest._
import org.bson.BsonString
import com.gardenShare.gardenshare.Persistentstorage.PersistenStorage
import cats.effect.IO
import com.gardenShare.gardenshare.Persistentstorage.DocumentData

object PersistentStorageSpec extends TestSuite {
  val tests = Tests{
    test("PersistenStorage"){
      test("for IO") {
        test("store") {
          test("Should use the provided driver to store some data") {
            val persistenStorage = PersistenStorage[IO]
            val testData = DocumentData(("index", BsonString("value")), Map.empty)
            implicit val mockDriver = new DocumentStoreDriver[IO] {
              def insertOneDocument(document: DocumentData, documentStoreLocation: DocumentStoreLocation): F[Unit] = (document, documentStoreLocation) match {
                case (testData == document) => IO.unit
              }
            }
            persistenStorage.store(testData).unsafeRunSync()
          }
        }
      }
    }
  }
}
