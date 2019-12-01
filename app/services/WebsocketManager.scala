package services

import akka.stream.scaladsl.{Source, SourceQueue}
import models.User

import scala.collection.concurrent.TrieMap

object WebsocketManager {
  sealed trait Status
  case object NotYetAuthorized extends Status
  case object Authorized extends Status

  //SessionId : (Status | Queue)
  val sockets = new TrieMap[String, (SourceQueue[String], Status)]()

  def addClient(jwt: String, pub: SourceQueue[String], status: Status) = sockets.update(jwt, (pub, status))

  def removeClient(jwt: String) = sockets.remove(jwt)
}
