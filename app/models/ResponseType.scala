package models

import io.circe.{Decoder, JsonObject}

object ResponseType {
  import io.circe.syntax._
  case object Ping extends ResponseType

  implicit val dec: io.circe.Decoder[ResponseType] = for {
    visitorType <- Decoder[String].prepare(_.downField("$type"))
    value <- visitorType match {
      case "Ping" => Decoder.const(Ping)
    }
  } yield { value }

  implicit val enc: io.circe.Encoder[ResponseType] = {
    case Ping => JsonObject.empty.add("$type", "Ping".asJson).asJson
  }
}

trait ResponseType
