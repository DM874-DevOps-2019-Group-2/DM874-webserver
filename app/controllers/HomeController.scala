package controllers

import java.util.UUID

import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import helper.{AkkaKafkaSendOnce, ClassLogger}
import javax.inject._
import models.{EventSourcingModel, UnauthedUser, User}
import play.api._
import play.api.mvc._
import schema.{DBUser, UsersDAO}
import security.{JWTAuthentication, JWTService}
import services.WebsocketManager

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                cc: ControllerComponents,
                                jwtAuthentication: JWTAuthentication,
                                jwtService: JWTService,
                                usersDAO: UsersDAO,
                                assets: Assets,
                                akkaKafkaSendOnce: AkkaKafkaSendOnce,
                              )(implicit mat: Materializer) extends AbstractController(cc) with ClassLogger {
  def userSocket = WebSocket.accept[String, String] { req =>
    import scala.concurrent.ExecutionContext.Implicits.global

    req.headers.get("dm874_jwt") match {
      case Some(jwt) => {
        //Create a queue'd source and bundle it with the request
        val out = Source.queue[String](1000, OverflowStrategy.backpressure).merge(Source.maybe[String])

        //Handle incoming through the socket
        val in = Sink.foreach[String]{ msg =>
          //Do something TODO

          //Decode the message to the request type


          //Db operation, get event source route for type
          val result: Future[Option[EventSourcingModel]] = ???

          result.map{
            case None => {
              logger.error(s"Failed to find event sourcing model for ${msg}")
            }
          }

        }

        Flow.fromSinkAndSourceMat[String, String, Future[akka.Done], SourceQueueWithComplete[String], Unit](in, out).apply{ case(_, queue) =>
          WebsocketManager.addClient(jwt, queue)
        }
      }
      case None => {
        //Return error TODO
        val out = Source.single[String](???)

        //Do nothing, no auth??
        val in = Sink.ignore

        Flow.fromSinkAndSource(in, out)
      }
    }


  }

  def greet(): Action[AnyContent] = Action { request =>
    Ok("Hello")
  }

  def register(): Action[AnyContent] = Action.async { implicit req =>
    import com.github.t3hnar.bcrypt._
    import io.circe.generic.auto._
    import io.circe.parser._

    req.body.asJson.map(_.toString()) match {
      case Some(s) => decode[UnauthedUser](s).toOption.map{ user =>
        val hashed = user.password.bcrypt
        usersDAO.insert(DBUser(None, user.username, hashed)).map(_ => Ok("Successfully registered user"))
      }.getOrElse(Future.successful(Ok("Failed to decode request")))
      case None => Future.successful(Ok("Failed to parse request"))
    }
  }

  def authedGreet(): Action[AnyContent] = jwtAuthentication { implicit req =>
    Ok("Authed successfully")
  }

  def login(): Action[AnyContent] = Action.async { implicit req =>
    import com.github.t3hnar.bcrypt._
    import io.circe.generic.auto._
    import io.circe.parser._
    import io.circe.syntax._

    req.body.asJson.map(_.toString()) match {
      case Some(s) => decode[UnauthedUser](s).toOption.map{ user =>
        usersDAO.get(user.username).map{
          case Some(dbUser) => {
            user.password.isBcryptedSafe(dbUser.password).toOption match {
              case Some(true) => {
                val newToken = jwtService.createToken(dbUser.asJson.noSpaces)
                Ok("Token:" + newToken)
              }
              case _ => Ok("Incorrect password")
            }
          }
          case None => Ok("Failed to find user")
        }
      }.getOrElse(Future.successful(Ok("Failed to decode request")))
      case None => Future.successful(Ok("Failed to parse request"))
    }
  }

  def fallback(path: String): Action[AnyContent] = Action { implicit req =>
    Redirect("/")
  }
}
