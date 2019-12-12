package services

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.Done
import akka.stream.QueueOfferResult
import com.google.cloud.storage.{BlobInfo, Storage}
import helper.{AkkaKafkaSendOnce, ClassLogger}
import models.{CodeSnippetNotification, EventSourcingModel, RequestType, ResponseType, User}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import security.JWTService
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MessageHandlerService (
                            jwtService: JWTService,
                            akkaKafkaSendOnce: AkkaKafkaSendOnce,
                            storage: Storage,
                            config: play.api.Configuration
                            )(implicit executionContext: ExecutionContext)
  extends ClassLogger {
  val codeSnippetBucket = config.getString("code-snippet.bucket").get
  val codeSnippetTopic = config.getString("code-snippet.topic").get

  def handleRequest(sessionId: String, user: User, requestType: RequestType): Future[akka.Done] = requestType match {
    case RequestType.SendMessage(message, destinationUsers) => {
      val tasks = sys.env("EVENT_DESTINATIONS").split(',').toSeq.map(_.split(':').toSeq).map{ case x1 :: x2 :: Nil => (x1, x2) }
      val messageId = java.util.UUID.randomUUID().toString

      val destinations = destinationUsers.map(destId => models.MessageDestination(
        destinationId = destId,
        message = message,
        messageId = messageId,
        fromAutoReply = false
      ))

      import io.circe.syntax._

      val outModel = EventSourcingModel(
        messageId = messageId,
        sessionId = sessionId,
        senderId = user.id,
        messageDestinations = destinations,
        eventDestinations = tasks.tail
      )

      akkaKafkaSendOnce.sendExactlyOnce(tasks.head._2, outModel.asJson.noSpaces)
    }
    //Generate filestore URL
    case RequestType.UploadHandlerSnippet => {
      import io.circe.syntax._

      akkaKafkaSendOnce.sendExactlyOnce(codeSnippetTopic, CodeSnippetNotification(codeSnippetBucket, user.id.toString, CodeSnippetNotification.Insert).asJson.noSpaces).flatMap{ _ =>
        val url = storage.signUrl(BlobInfo.newBuilder(codeSnippetBucket, user.id.toString).build(), 1, TimeUnit.HOURS).toString

        val offer = WebsocketManager.sockets.get(sessionId).map(_.offer((ResponseType.CodeSnippetUploadUrl(url): ResponseType).asJson.noSpaces)).getOrElse(Future.successful(QueueOfferResult.Failure(new Exception("Failed to find socket"))))

        offer.map { x => x match {
          case QueueOfferResult.Enqueued => akka.Done
          case _ => {
            logger.error(s"Failed to publish item, queue said ${x}")
            akka.Done
          }
        }}
      }
    }
  }
}
