package controllers

import java.util.UUID

import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import helper.{AkkaKafkaSendOnce, ClassLogger}
import javax.inject._
import models.{EventSourcingModel, RequestType, UnauthedUser, User, UserWithSession}
import play.api._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import schema.{DBUser, UsersDAO, UsersTable}
import security.{JWTAuthentication, JWTService}
import services.WebsocketManager
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


@Singleton
class WebsocketController @Inject()(
                                cc: ControllerComponents,
                                jwtAuthentication: JWTAuthentication,
                                jwtService: JWTService,
                                usersDAO: UsersDAO,
                                assets: Assets,
                                configuration: play.api.Configuration,
                                akkaKafkaSendOnce: AkkaKafkaSendOnce,
                                protected val dbConfigProvider: DatabaseConfigProvider,
                                dependencyInjector: services.DependencyInjector
                              )(implicit mat: Materializer) extends AbstractController(cc) with ClassLogger with HasDatabaseConfigProvider[JdbcProfile] {
  def userSocket = WebSocket.accept[String, String] { req =>
    import io.circe.syntax._

    val oUser = (req.headers.get("dm874_jwt") match {
      case Some(jwt) => {
        //Authenticate JWT
        (dependencyInjector.jwtService.tryDecode(jwt) match {
          case Failure(e) => Left(e)
          case Success(claims) => {
            import io.circe.parser._
            decode[User](claims)
          }
        }) match {
          case Left(e) => {
            logger.error(s"Failed to decode JWT with error ${e}")
            None
          }
          case Right(user) => Some(user)
        }
      }
      case None => None
    })

    oUser match {
      case None => {
        val out = Source.single[String](models.Error("Failed to authenticate user, please try again").asJson.noSpaces)

        //Do nothing, no auth??
        val in = Sink.ignore

        Flow.fromSinkAndSource(in, out)
      }
      case Some(user) => {
        val sessionId = user.id.toString + java.util.UUID.randomUUID().toString

        //Create a queue'd source and bundle it with the request
        val authTopic = configuration.get[String]("auth.topic")

        val out = Source.fromFutureSource(akkaKafkaSendOnce.sendExactlyOnce(authTopic, UserWithSession(sessionId, user).asJson.noSpaces).map(_ =>
          Source.queue[String](1000, OverflowStrategy.backpressure).merge(Source.maybe[String])
        ))

        //Handle incoming through the socket
        val in = Sink.foreachAsync[String](2){ msg =>
          //Decode the message to the request type
          import io.circe.parser._

          val f = decode[models.RequestType](msg) match {
            case Left(e) => {
              logger.error(s"Error: ${e}")
              Future.successful(akka.Done)
            }
            case Right(rt) => {
              //Check if the user is authed
              WebsocketManager.sockets.get(sessionId) match {
                case None => {
                  logger.error(s"Error: Failed to get user for sessionId $sessionId")
                  Future.successful(akka.Done)
                }
                case Some((queue, status)) => status match {
                  case WebsocketManager.Authorized => dependencyInjector.messageHandlerService.handleRequest(sessionId, user, rt)
                  case x => {
                    logger.error(s"Error: User not authorized, status is ${x}")
                    Future.successful(akka.Done)
                  }
                }
              }

            }
          }

          f.map(_ => ())
        }

        Flow.fromSinkAndSourceMat[String, String, Future[akka.Done], Future[SourceQueueWithComplete[String]], Unit](in, out).apply { case (_, q) =>
          q.map(queue => WebsocketManager.addClient(sessionId, queue, WebsocketManager.NotYetAuthorized))
        }
      }
    }
  }
}
