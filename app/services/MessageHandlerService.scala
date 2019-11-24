package services

import akka.Done
import helper.{AkkaKafkaSendOnce, ClassLogger}
import models.{EventSourcingModel, RequestType, User}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import schema.UsersTable
import security.JWTService
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MessageHandlerService (
                            jwtService: JWTService,
                            protected val dbConfigProvider: DatabaseConfigProvider,
                            akkaKafkaSendOnce: AkkaKafkaSendOnce,
                            fileStore: FileStore,
                            config: play.api.Configuration
                            )(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with ClassLogger {
  import dbConfig.profile._
  import dbConfig.profile.api._

  def handleRequest(sessionId: String, user: User, requestType: RequestType): Future[akka.Done] = requestType match {
    case RequestType.SendMessage(message, destinationUsers) => {
      val dbOp: Future[Option[Seq[(Int, String)]]] = ???

      dbOp.flatMap{
        //Handle by directing it onwards
        case None => {
          logger.error(s"Failed to find EventSourcingModel")
          Future.successful(akka.Done)
        }
        case Some(t) => {
          val tasks = t.sortBy(_._1)
          val messageId = java.util.UUID.randomUUID().toString

          val destinations = destinationUsers.map(destId => models.OutboundMessage(
            destinationId = destId,
            message = message,
            messageId = messageId,
            fromAutoReply = false
          ))

          import io.circe.syntax._

          val outModel = EventSourcingModel(
            messageId = messageId,
            sender = user,
            messageDestinations = destinations,
            tasks = tasks.tail
          )

          akkaKafkaSendOnce.sendExactlyOnce(tasks.head._2, outModel.asJson.noSpaces)
        }
      }
    }
      //Handle it here
    case RequestType.ChangePassword(newPassword) => {
      import com.github.t3hnar.bcrypt._
      db.run(UsersTable.users.filter(_.id === user.id).filter(_.username === user.username).map(_.password).update(newPassword.bcrypt)).map(_ => Done)
    }
      //File store upload, then notification
    case RequestType.UploadHandlerSnippet => {
      //Todo Handle data?
      fileStore.putAsync(config.get[String]("user.scripts.bucket"), user.id.toString, "")
    }
  }
}
