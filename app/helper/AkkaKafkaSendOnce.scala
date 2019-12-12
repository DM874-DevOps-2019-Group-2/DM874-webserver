package helper

import akka.actor.ActorSystem
import akka.{Done, kafka}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import javax.inject.Singleton
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

class AkkaKafkaSendOnce (
                                 config: play.api.Configuration
                                 )(implicit actorSystem: ActorSystem, val materializer: Materializer) {
  import scala.concurrent.ExecutionContext.Implicits.global
  val producerSettings = ProducerSettings(config.underlying.getConfig("akka.kafka.producer"), new StringSerializer, new StringSerializer)
      .withBootstrapServers(config.get[String]("kafka.bootstrap.servers"))


  def sendExactlyOnce(topic: String, data: String): Future[Done] = {
    val pr = new ProducerRecord[String, String](topic, data)

    Source.single(pr)
      .runWith(Producer.plainSink(producerSettings))
  }
}