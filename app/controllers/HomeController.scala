package controllers

import java.util.UUID

import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import helper.{AkkaKafkaSendOnce, ClassLogger}
import javax.inject._
import models.{EventSourcingModel, RequestType, UnauthedUser, User}
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
                                protected val dbConfigProvider: DatabaseConfigProvider,
                                dependencyInjector: services.DependencyInjector
                              )(implicit mat: Materializer) extends AbstractController(cc) with ClassLogger with HasDatabaseConfigProvider[JdbcProfile] {
  def userSocket = WebSocket.accept[String, String] { req =>
    import scala.concurrent.ExecutionContext.Implicits.global

    //Try to decode JWT, then decode it from json and finally check the db for correctness
    val authedUser = (req.headers.get("dm874_jwt") match {
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
    }) match {
      case Some(user) => {
        import profile.api._
        db.run(UsersTable.users.filter(_.id === user.id).filter(_.username === user.username).exists.result).map {
          case true => Some(user)
          case false => None
        }
      }
      case None => {
        logger.error(s"Failed to decode user")
        Future.successful(None)
      }
    }

    //Now we convert the future to a source that either instantly closes or streams
    import io.circe.syntax._

    val fut = authedUser.map {
      case None => {
        val out = Source.single[String](models.Error("Failed to authenticate user, please try again").asJson.noSpaces)

        //Do nothing, no auth??
        val in = Sink.ignore

        Flow.fromSinkAndSource(in, out)
      }
      case Some(user) => {
        val sessionId = user.id.toString + java.util.UUID.randomUUID().toString

        //Create a queue'd source and bundle it with the request
        val out = Source.queue[String](1000, OverflowStrategy.backpressure).merge(Source.maybe[String])

        //Handle incoming through the socket
        val in = Sink.foreach[String]{ msg =>
          //Decode the message to the request type
          import io.circe.syntax._
          import io.circe.parser._

          decode[models.RequestType](msg) match {
            case Left(e) => {
              logger.error(s"Error: ${e}")
              Future.successful(akka.Done)
            }
            case Right(rt) => dependencyInjector.messageHandlerService.handleRequest(sessionId, user, rt)
          }
        }

        Flow.fromSinkAndSourceMat[String, String, Future[akka.Done], SourceQueueWithComplete[String], Unit](in, out).apply { case (_, queue) =>
          WebsocketManager.addClient(sessionId, queue)
        }
      }
    }

    //Since the flow is dynamic by operation, we have to wait for the future. Unfortunately play has not implemented a way to invoke the wrapping flow's mapAsync, so therefore we must wait for the future
    Await.result(fut, scala.concurrent.duration.Duration.Inf)
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
