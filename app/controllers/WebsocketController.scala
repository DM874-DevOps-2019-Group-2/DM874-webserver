package controllers

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import helper.{AkkaKafkaSendOnce, ClassLogger}
import javax.inject._
import models.{EventSourcingModel, RequestType, ResponseType, UnauthedUser, User, UserWithSession}
import play.api._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import schema.{DBUser, UsersDAO, UsersTable}
import sdis.operations.StringOps
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
                                assets: Assets,
                                configuration: play.api.Configuration,
                                akkaKafkaSendOnce: AkkaKafkaSendOnce,
                                protected val dbConfigProvider: DatabaseConfigProvider,
                                dependencyInjector: services.DependencyInjector
                              )(implicit mat: Materializer, system: ActorSystem) extends AbstractController(cc) with ClassLogger with HasDatabaseConfigProvider[JdbcProfile] {
  val listenTopic = configuration.getString("kafka.streams.topic").get
  val websocketTtl = configuration.getInt("connection.ttlseconds").get

  def userSocket = WebSocket.accept[String, String] { req =>
    import io.circe.syntax._

    //Parse JWT
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

    import scala.concurrent.duration._

    oUser match {
      //Do nothing
      case None => {
        val out = Source.single[String](models.Error("Failed to authenticate user, please try again").asJson.noSpaces)

        //Do nothing, no auth??
        val in = Sink.ignore

        Flow.fromSinkAndSource(in, out)
      }
      case Some(user) => {
        val sessionId = user.id.toString + java.util.UUID.randomUUID().toString
        val expireQuery = StringOps.expire(user.id.toString, websocketTtl)

        //Create a queue'd source and bundle it with the request
        val out = {
          Source.queue[String](1000, OverflowStrategy.backpressure).merge(Source.maybe[String])
            //Ping every ttl*0.8 time
            .merge(Source(0 to Int.MaxValue).throttle(1, (websocketTtl * 0.8) seconds).map(_ => (ResponseType.Ping: ResponseType).asJson.noSpaces))
            //on every request, refresh ttl
            .map { x =>
              dependencyInjector.redisClient.run(expireQuery)
              WebsocketManager.updateTTL(sessionId, websocketTtl seconds)
              x
          }
        }

        //Handle incoming through the socket
        val in = Sink.foreachAsync[String](2){ msg =>
          //Decode the message to the request type
          import io.circe.parser._

          val f = decode[models.RequestType](msg) match {
            case Left(e) => {
              logger.error(s"Error: ${e}")
              Future.successful(akka.Done)
            }
            case Right(rt) => dependencyInjector.messageHandlerService.handleRequest(sessionId, user, rt)
          }

          f.map(_ => ())
        }

        Flow.fromSinkAndSourceMat[String, String, Future[akka.Done], SourceQueueWithComplete[String], Unit](in, out).apply { case (_, q) =>
          val insQs = sdis.operations.SetOps.sadd(user.id.toString, listenTopic)
          dependencyInjector.redisClient.run(insQs)
          dependencyInjector.redisClient.run(expireQuery)
          WebsocketManager.addClient(sessionId, q, websocketTtl seconds)
        }
      }
    }
  }
}
