package models

object RequestTypes {
  case object SendMessage extends RequestType
  case object ChangePassword extends RequestType
  case object UploadHandlerSnippet extends RequestType
}

sealed trait RequestType