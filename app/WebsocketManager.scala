import akka.stream.scaladsl.Source
import models.User

import scala.collection.concurrent.TrieMap

object WebsocketManager {
  val sockets = new TrieMap[String, Source[]]()

  def addClient(user: User) = {
  }

  def removeClient(user: User) = {}
}
