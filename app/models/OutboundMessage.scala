package models

object OutboundMessage {
  implicit val dec: io.circe.Decoder[OutboundMessage] = io.circe.generic.semiauto.deriveDecoder[OutboundMessage]
  implicit val enc: io.circe.Encoder[OutboundMessage] = io.circe.generic.semiauto.deriveEncoder[OutboundMessage]
}

case class OutboundMessage(
                          destinationId: Int,
                          messageId: String,
                          message: String,
                          fromAutoReply: Boolean
                          )
