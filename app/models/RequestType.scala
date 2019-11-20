package models

import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._

object RequestType {
  case class SendMessage(payload: JsonObject) extends RequestType
  case class ChangePassword(payload: JsonObject) extends RequestType
  case class UploadHandlerSnippet(payload: JsonObject) extends RequestType

  //Todo
  /*
    implicit val enc: ObjectEncoder[DropdownItem] = ObjectEncoder.instance {
    case u : Entry => u.asJsonObject.add("$type", "Entry".asJson)
    case u : Separator.type => JsonObject.empty.add("$type", "Separator".asJson)
  }

  implicit val dec: Decoder[DropdownItem] = for {
    visitorType <- Decoder[String].prepare(_.downField("$type"))
    value <- visitorType match {
      case "Entry" => Decoder[Entry]
      case "Separator" => Decoder.const(Separator)
      case other => Decoder.failedWithMessage(s"invalid type: $other")
    }
  } yield value
   */

  implicit val dec: io.circe.Decoder[RequestType] = (c: HCursor) => (for {
    x <- *.get(c.value.noSpaces)
  } yield { x }) match {
    case None => Left(DecodingFailure(s"Failed to find matching type-tag for value ${c.value}", List.empty))
    case Some(result) => Right(result)
  }

  implicit val enc: io.circe.Encoder[RequestType] = {
    case SendMessage => JsonObject.empty.add("$type", "SendMessage".asJson).asJson
    case ChangePassword => JsonObject.empty.add("$type", "ChangePassword".asJson).asJson
    case UploadHandlerSnippet => JsonObject.empty.add("$type", "UploadHandlerSnippet".asJson).asJson
  }
}

sealed trait RequestType {
  def payload: JsonObject
}