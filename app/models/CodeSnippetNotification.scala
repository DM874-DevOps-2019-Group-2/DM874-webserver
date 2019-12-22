package models

case class CodeSnippetNotification(
                                  userId: Int,
                                  target: CodeSnippetNotification.Target,
                                  actionType: CodeSnippetNotification.OpType
                                  )

object CodeSnippetNotification {
  trait OpType
  case object enable extends OpType
  case object disable extends OpType

  import io.circe.syntax._

  implicit val opEnc: io.circe.Encoder[OpType] = {
    case enable => "enable".asJson
    case disable => "disable".asJson
  }

  trait Target
  case object send extends Target
  case object recv extends Target

  import io.circe.syntax._

  implicit val tarEnc: io.circe.Encoder[Target] = {
    case send => "send".asJson
    case recv => "recv".asJson
  }

  implicit val enc: io.circe.ObjectEncoder[CodeSnippetNotification] = io.circe.generic.semiauto.deriveEncoder[CodeSnippetNotification]
}