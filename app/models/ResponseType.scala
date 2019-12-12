package models

import io.circe.{Decoder, JsonObject}

object ResponseType {
  import io.circe.syntax._
  case object Ping extends ResponseType
  case class CodeSnippetUploadUrl(url: String) extends ResponseType
  case class ReceiveMessage(msg: String) extends ResponseType

  implicit val enc: io.circe.Encoder[ResponseType] = {
    case Ping => JsonObject.empty.add("$type", "Ping".asJson).asJson
    case x:CodeSnippetUploadUrl => {
      implicit val localEnc: io.circe.ObjectEncoder[CodeSnippetUploadUrl] = io.circe.generic.semiauto.deriveEncoder[CodeSnippetUploadUrl]
      x.asJsonObject.add("$type", "CodeSnippetUploadUrl".asJson).asJson
    }
    case x:ReceiveMessage => {
      implicit val localEnc: io.circe.ObjectEncoder[ReceiveMessage] = io.circe.generic.semiauto.deriveEncoder[ReceiveMessage]
      x.asJsonObject.add("$type", "ReceiveMessage".asJson).asJson
    }
  }
}

trait ResponseType
