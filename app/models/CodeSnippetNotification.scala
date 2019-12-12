package models

case class CodeSnippetNotification(
                                  bucket: String,
                                  key: String,
                                  opType: CodeSnippetNotification.OpType
                                  )

object CodeSnippetNotification {
  trait OpType
  case object Insert extends OpType
  case object Remove extends OpType

  import io.circe.syntax._

  implicit val opEnc: io.circe.Encoder[OpType] = {
    case Insert => "insert".asJson
    case Remove => "remove".asJson
  }

  implicit val enc: io.circe.ObjectEncoder[CodeSnippetNotification] = io.circe.generic.semiauto.deriveEncoder[CodeSnippetNotification]
}