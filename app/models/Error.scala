package models

case class Error(
                message: String
                )

object Error {
  implicit val dec: io.circe.Decoder[Error] = io.circe.generic.semiauto.deriveDecoder[Error]
  implicit val enc: io.circe.Encoder[Error] = io.circe.generic.semiauto.deriveEncoder[Error]
}
