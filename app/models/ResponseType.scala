package models

import io.circe.{Decoder, JsonObject}

object ResponseType {
  import io.circe.syntax._
  case object Ping extends ResponseType
  case object CodeSnippetUploaded extends ResponseType
  case class ReceiveMessage(msg: String, userId: Int) extends ResponseType

  implicit val enc: io.circe.Encoder[ResponseType] = {
    case Ping => JsonObject.empty.add("$type", "Ping".asJson).asJson
    case CodeSnippetUploaded => JsonObject.empty.add("$type", "CodeSnippetUploaded".asJson).asJson
    case x:ReceiveMessage => {
      implicit val localEnc: io.circe.ObjectEncoder[ReceiveMessage] = io.circe.generic.semiauto.deriveEncoder[ReceiveMessage]
      x.asJsonObject.add("$type", "ReceiveMessage".asJson).asJson
    }
  }
}

trait ResponseType
