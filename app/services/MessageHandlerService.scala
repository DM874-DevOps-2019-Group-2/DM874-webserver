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
  val routeTopic = config.getString("router.topic").get

  def handleRequest(sessionId: String, user: User, requestType: RequestType): Future[akka.Done] = requestType match {
    case RequestType.SendMessage(message, destinationUsers) => {
      val messageId = java.util.UUID.randomUUID().toString

      import io.circe.syntax._

      val outModel = EventSourcingModel(
        messageId = messageId,
        sessionId = sessionId,
        senderId = user.id,
        messageBody = message,
        recipientIds = destinationUsers,
        eventDestinations = Seq(routeTopic),
        fromAutoReply = false
      )

      akkaKafkaSendOnce.sendExactlyOnce(outModel.eventDestinations.head, outModel.asJson.noSpaces)
    }
    //Generate filestore URL
    case RequestType.UploadHandlerSnippet => {
      import io.circe.syntax._

      akkaKafkaSendOnce.sendExactlyOnce(codeSnippetTopic, CodeSnippetNotification(user.id, CodeSnippetNotification.recv, CodeSnippetNotification.enable).asJson.noSpaces).flatMap{ _ =>
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
