package models

object EventSourcingModel {
  implicit val dec: io.circe.Decoder[EventSourcingModel] = io.circe.generic.semiauto.deriveDecoder[EventSourcingModel]
  implicit val enc: io.circe.Encoder[EventSourcingModel] = io.circe.generic.semiauto.deriveEncoder[EventSourcingModel]
}

case class EventSourcingModel(
                             messageId: String,
                             sender: User,
                             messageDestinations: Seq[OutboundMessage],
                             tasks: Seq[(Int, String)]
                             )
