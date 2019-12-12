package models

import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._

object RequestType {
  case class SendMessage(message: String, destinationUsers: Seq[Int]) extends RequestType
  case object UploadHandlerSnippet extends RequestType

  object SendMessage {
    implicit val dec: io.circe.Decoder[SendMessage] = io.circe.generic.semiauto.deriveDecoder[SendMessage]
    implicit val enc: io.circe.Encoder.AsObject[SendMessage] = io.circe.generic.semiauto.deriveEncoder[SendMessage]
  }


  implicit val dec: io.circe.Decoder[RequestType] = for {
    visitorType <- Decoder[String].prepare(_.downField("$type"))
    value <- visitorType match {
      case "SendMessage" =>  Decoder[SendMessage]
      case "UploadHandlerSnippet" => Decoder.const(UploadHandlerSnippet)
    }
  } yield { value }

  implicit val enc: io.circe.Encoder[RequestType] = {
    case x: SendMessage => x.asJsonObject.add("$type", "SendMessage".asJson).asJson
    case UploadHandlerSnippet => JsonObject.empty.add("$type", "UploadHandlerSnippet".asJson).asJson
  }
}

sealed trait RequestType