package models

object MessageDestination {
  implicit val dec: io.circe.Decoder[MessageDestination] = io.circe.generic.semiauto.deriveDecoder[MessageDestination]
  implicit val enc: io.circe.Encoder[MessageDestination] = io.circe.generic.semiauto.deriveEncoder[MessageDestination]
}

case class MessageDestination(
                             senderId: Int,
                          destinationId: Int,
                          message: String
                          )
