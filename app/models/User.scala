package models

case class User (
                id: Int,
                username: String
               )

object User {
  implicit val dec: io.circe.Decoder[User] = io.circe.generic.semiauto.deriveDecoder[User]
  implicit val enc: io.circe.Encoder[User] = io.circe.generic.semiauto.deriveEncoder[User]
}

