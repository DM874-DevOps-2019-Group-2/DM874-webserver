package services

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Inject
import helper.AkkaKafkaSendOnce
import play.api.db.slick.DatabaseConfigProvider
import security.JWTService

@Singleton
class DependencyInjector @Inject()(
                                  config: play.api.Configuration,
                                  protected val dbConfigProvider: DatabaseConfigProvider,
                                  )(implicit system: ActorSystem, mat: Materializer) {
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val fileStore = new FileStore()
  lazy val jwtService = new JWTService(config)
  lazy val messageHandlerService = new MessageHandlerService(jwtService, dbConfigProvider, akkaKafkaSendOnce, fileStore)
  lazy val akkaKafkaSendOnce = new AkkaKafkaSendOnce(config)
}
