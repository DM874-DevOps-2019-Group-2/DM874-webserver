package services

import akka.stream.scaladsl.{Source, SourceQueue}
import models.User

import scala.collection.concurrent.TrieMap

object WebsocketManager {
  //SessionId : Queue
  val sockets = new TrieMap[String, SourceQueue[String]]()

  def addClient(jwt: String, pub: SourceQueue[String]) = sockets.update(jwt, pub)

  def removeClient(jwt: String) = sockets.remove(jwt)
}
