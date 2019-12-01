package models

case class User (
                id: Int,
                username: String,
                jwt: String
               )

object User {
  implicit val dec: io.circe.Decoder[User] = io.circe.generic.semiauto.deriveDecoder[User]
  implicit val enc: io.circe.Encoder[User] = io.circe.generic.semiauto.deriveEncoder[User]
}

case class UserWithSession(
                          sessionId: String,
                          user: User
                          )

object UserWithSession {
  implicit val dec: io.circe.Decoder[UserWithSession] = io.circe.generic.semiauto.deriveDecoder[UserWithSession]
  implicit val enc: io.circe.Encoder[UserWithSession] = io.circe.generic.semiauto.deriveEncoder[UserWithSession]
}

case class UnauthedUser(
                       username: String,
                       password: String
                       )
