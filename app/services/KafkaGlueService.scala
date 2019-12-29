package services

import helper.{ClassLogger, KafkaHelper}
import kafka.Entrypoint
import models.{MessageDestination, ResponseType}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class KafkaGlueService(
                        config: com.typesafe.config.Config,
                        entrypoint: Entrypoint,
                        kafkaHelper: KafkaHelper
                      )(implicit ec: ExecutionContext) extends ClassLogger {
  kafkaHelper.createTopics(Seq(
    config.getString("kafka.streams.topic")
  ))

  val started = entrypoint.start{ case (k, v) =>
    Try {
      import io.circe.syntax._
      import io.circe.parser._
      val out = decode[MessageDestination](v) match {
        case Left(value) => {
          logger.error(s"Failed to decode message with error ${value}")
          Future.successful(akka.Done)
        }
        case Right(value) => {
          val data = (ResponseType.ReceiveMessage(value.message, value.senderId): ResponseType).asJson.noSpaces

          WebsocketManager.userSessions.get(value.destinationId) match {
            case None => {
              logger.error(s"Expected to find user session, found none")
              Future.successful(akka.Done)
            }
            case Some(value) => Future.sequence(value.map{ sessionId =>
              WebsocketManager.sockets.get(sessionId) match {
                case None => {
                  logger.error(s"Failed to get socket for sessionId ${sessionId}")
                  Future.successful(akka.Done)
                }
                case Some(pub) => pub.offer(data).map(_ => akka.Done)
              }
            }).map(_ => akka.Done)
          }
        }
      }

      Try {
        Await.result(out, scala.concurrent.duration.Duration.Inf)
      }

      Seq.empty[(String, String)]
    } match {
      case Failure(exception) => {
        logger.error(s"Failed in kafka streams with exception ${exception}")
        Seq.empty
      }
      case Success(value) => value
    }
  }
}
