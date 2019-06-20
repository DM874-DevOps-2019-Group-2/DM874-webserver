package security

import play.api.Configuration
import javax.inject._
import pdi.jwt.{Jwt, JwtAlgorithm}

import scala.util.Try


@Singleton
class JWTService @Inject()(config: Configuration) {
  val sc = config.get[String]("play.http.secret.key")
  val algo = "HS256"

  def createToken(payload: String): String = Jwt.encode(payload, sc, JwtAlgorithm.HS256)

  def tryDecode(jwtToken: String): Try[String] = Jwt.decodeRaw(jwtToken, sc, Seq(JwtAlgorithm.HS256))
}
