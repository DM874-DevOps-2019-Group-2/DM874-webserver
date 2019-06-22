package controllers

import javax.inject._
import models.User
import play.api._
import play.api.mvc._
import security.{JWTAuthentication, JWTService}

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
                                jwtService: JWTService
                              ) extends AbstractController(cc) {
  def greet() = Action { request =>
    Ok("Hello")
  }

  def authedGreet() = jwtAuthentication { implicit req =>
    Ok("Authed successfully")
  }

  def login() = Action.async { implicit req =>
    import io.circe.generic.auto._
    import io.circe.syntax._
    val newToken = jwtService.createToken(User("valde").asJson.noSpaces)
    Future.successful(Ok(s"Token is ${newToken}"))
  }
}
