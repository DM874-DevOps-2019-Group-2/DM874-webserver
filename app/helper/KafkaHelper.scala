package helper

import java.util.Properties

import org.apache.kafka.clients.admin.NewTopic

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

class KafkaHelper (
                    kafkaProperties: KafkaHelper.KafkaProperties
                  ) extends ClassLogger {
  def createTopics(topics: Seq[String]) = {
    logger.info(s"Creating topics ${topics} on with properties ${kafkaProperties()}")
    Try {
      val adminClient = org.apache.kafka.clients.admin.AdminClient.create(kafkaProperties())
      val r = adminClient.createTopics(topics.map(x => new NewTopic(x, 1, 1)).asJava)

      r.values().asScala.foreach(_._2.get())
      r.all().get()
      logger.info(s"${topics} have been created")
    } match {
      case Success(_) => ()
      case Failure(exception) => logger.warn("Failed to create topics, they may already exist: See exception:", exception)
    }
  }
}

object KafkaHelper {
  case class KafkaProperties(
                              config: play.api.Configuration
                            ) {
    def apply(): Properties = {
      val p = new Properties()
      p.put("bootstrap.servers", config.get[String]("kafka.bootstrap.servers"))
      p
    }
  }
}