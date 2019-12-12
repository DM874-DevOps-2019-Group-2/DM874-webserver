package services

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.cloud.storage.StorageOptions
import com.google.inject.Inject
import helper.{AkkaKafkaSendOnce, KafkaHelper}
import kafka.Entrypoint
import play.api.db.slick.DatabaseConfigProvider
import security.JWTService
import sdis.client.RedisClient
import com.google.inject._


@Singleton
class DependencyInjector @Inject()(
                                  config: play.api.Configuration,
                                  )(implicit system: ActorSystem, mat: Materializer) {
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val redisClient = RedisClient.getInstance(config.getString("redis.hostname").get, config.getInt("redis.port").get).get

  lazy val kafkaHelper = new KafkaHelper(KafkaHelper.KafkaProperties(config))
  lazy val entrypoint = new Entrypoint(config)

  val kafkaGlue = new KafkaGlueService(config.underlying, entrypoint, kafkaHelper)
  lazy val storage = StorageOptions.getDefaultInstance.getService

  lazy val jwtService = new JWTService(config)
  lazy val messageHandlerService = new MessageHandlerService(jwtService, akkaKafkaSendOnce, storage, config)
  lazy val akkaKafkaSendOnce = new AkkaKafkaSendOnce(config)
}
