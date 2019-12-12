package kafka

import java.util.{Properties, UUID}

import helper.ClassLogger
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.processor.{RecordContext, TopicNameExtractor}
import org.apache.kafka.streams.scala.{ByteArrayKeyValueStore, StreamsBuilder}
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, Materialized, Produced}
import org.apache.kafka.streams.state.{QueryableStoreTypes, Stores}

object Entrypoint {
  type OutputTopic = String

  type Key = String
  type Data = String

  type DynamicallyRoutedData = Seq[(Data, OutputTopic)]
  type RoutedData = (Data, OutputTopic)
}

class Entrypoint (
                   configuration: play.api.Configuration
                 ) extends ClassLogger {
  import Entrypoint._

  import org.apache.kafka.streams.scala.Serdes._
  import org.apache.kafka.streams.scala.ImplicitConversions._
  import scala.collection.JavaConverters._

  // We need the second element of the tuple, for dynamic topic names.
  object TupleSerde {

    class DynamicTopicSerializer extends Serializer[RoutedData] {
      override def serialize(topic: OutputTopic, data: RoutedData): Array[Byte] = String.serializer().serialize(topic, data._1)

      override def close(): Unit = super.close()
    }

    class DynamicTopicSerde extends Serde[RoutedData] {
      override def serializer(): Serializer[(Data, OutputTopic)] = new DynamicTopicSerializer

      override def deserializer(): Deserializer[(Data, OutputTopic)] = ???
    }

    val serde = new DynamicTopicSerde
  }
  val config = configuration.underlying

  //The application conf contains the correctly mapped key -> value pair under the "kafka" subconfiguration
  private val properties = {
    val p = new Properties()
    val kafkaConfig = config.getConfig("kafka.streams.direct-arguments")
    kafkaConfig.entrySet().asScala.toSeq.map(x => (x.getKey, kafkaConfig.getString(x.getKey))).foreach { case (k, v) => p.setProperty(k, v) }
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, UUID.randomUUID().toString)
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString("kafka.bootstrap.servers"))
    p.put(StreamsConfig.STATE_DIR_CONFIG, config.getString("kafka.streams.statedir"))
    println(s"Starting Kafka streams entrypoint with $p")
    p.put("commit.interval.ms", "5000")
    p.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    p.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    p.put("num.stream.threads", "4")
    p
  }

  // We create a builder, this is the "context" that the streams will run under
  private val builder = new StreamsBuilder

  // We create two readers
  private val stream = builder.stream[Key, Data](topic = config.getString("kafka.streams.topic"))

  type StartHandler = (Key, Data) => DynamicallyRoutedData

  def start(handler: StartHandler): (() => Unit, KafkaStreams) = {
    stream
      .map { case (k, v) =>
        (k, handler(k, v))
      }
      .flatMapValues(x => x)
      .to((_: Key, value: RoutedData, _: RecordContext) => {
        value._2
      })(Produced.`with`(org.apache.kafka.streams.scala.Serdes.String, TupleSerde.serde))
    val materializedStream = new KafkaStreams(builder.build(), properties)

    // Non-blocking
    materializedStream.start()

    // We must wait for the cancel stream
    while (materializedStream.state() != KafkaStreams.State.RUNNING) {
      Thread.sleep(150)
    }
    println("Streams have started")

    val f = () => {
      materializedStream.close()
      materializedStream.cleanUp()
    }

    (f, materializedStream)
  }
}