package models

object EventSourcingModel {
  implicit val dec: io.circe.Decoder[OutboundMessage] = io.circe.generic.semiauto.deriveDecoder[OutboundMessage]
  implicit val enc: io.circe.Encoder[OutboundMessage] = io.circe.generic.semiauto.deriveEncoder[OutboundMessage]
}

case class EventSourcingModel(
                             messageId: Int,
                             sender: User,
                             messageDestinations: Seq[OutboundMessage],
                             tasks: Seq[(Int, String)]
                             )
