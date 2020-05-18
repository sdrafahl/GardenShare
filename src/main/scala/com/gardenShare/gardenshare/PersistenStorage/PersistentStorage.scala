package com.gardenShare.gardenshare.Persistentstorage

import com.gardenShare.gardenshare.Config.Config
import cats.effect.IO
import scala.jdk.CollectionConverters._
import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import org.mongodb.scala.MongoClient
import com.gardenShare.gardenshare.Config.MongoSettings
import scala.language.implicitConversions
import cats.effect.Async
import org.mongodb.scala.Document
import org.bson.BsonDocument
import org.bson.BsonValue
import org.bson.BsonElement

case class DocumentStoreLocation(databaseName: String, collectionName: String)
case class DocumentData(index: (String, BsonValue), values:Map[String, BsonValue])

object DocumentData {
  abstract class DocumentDataToMongoDocument {
    def toDocument(doc:DocumentData): Document
  }
  implicit object DocumentDataToMongoDocumentInstance extends DocumentDataToMongoDocument {
    def toDocument(doc:DocumentData): Document = Document.fromSeq((doc.values ++ Map(doc.index)).toSeq)
  }
  implicit class DocumentDataToMongoDocumentInstanceTypeClass(documentData:DocumentData) {
    def toMongoDocument(implicit documentDataToMongoDocument :DocumentDataToMongoDocument) = documentDataToMongoDocument.toDocument(documentData)
  }
}

abstract class PersistenStorage[F[_]] {
  def store(document: DocumentData ,documentStoreLocation: DocumentStoreLocation)(implicit driver: DocumentStoreDriver[F]): F[Unit]
}

object PersistenStorage {
  def apply[F[_]: PersistenStorage] = implicitly[PersistenStorage[F]]

  implicit object IOPersistenStorage extends PersistenStorage[IO] {
    def store(document: DocumentData ,documentStoreLocation: DocumentStoreLocation)(implicit driver: DocumentStoreDriver[IO]): IO[Unit] = ???
  }
}

abstract class DocumentStoreDriver[F[_]] {
  def insertOneDocument(document: DocumentData, documentStoreLocation: DocumentStoreLocation): F[Unit]
}

object DocumentStoreDriver {
  implicit def apply[F[_]: Async](mongoSettings: MongoSettings) = new DocumentStoreDriver[F] {
    val mongoConnectionString = s"mongodb://${mongoSettings.host}:${mongoSettings.password}@${mongoSettings.host}:${mongoSettings.port}"
    val client: MongoClient = MongoClient(mongoConnectionString)

    def insertOneDocument(document: DocumentData, documentStoreLocation: DocumentStoreLocation): F[Unit] = {
      Async[F].async { cb =>
        cb(Right(client.getDatabase(documentStoreLocation.databaseName).getCollection(documentStoreLocation.collectionName).insertOne(document.toMongoDocument)))
      }
    }
  }
}

