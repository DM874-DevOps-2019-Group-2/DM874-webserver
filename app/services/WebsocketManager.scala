package services

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.scaladsl.{Source, SourceQueue}
import models.User

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object WebsocketManager {
  //SessionId : Queue
  type SessionId = String

  val userSessions = new TrieMap[Int, Set[SessionId]]
  val sockets = new TrieMap[SessionId, SourceQueue[String]]()
  val sessionSuicide = new TrieMap[SessionId, Cancellable]()

  def updateTTL(sessionId: String, ttl: Duration)(implicit ec: ExecutionContext, system: ActorSystem) = {
    sessionSuicide.remove(sessionId).map(_.cancel())

    sessionSuicide.update(sessionId, system.scheduler.scheduleOnce(ttl) {
      sockets.remove(sessionId)
      sessionSuicide.remove(sessionId)
    })
  }

  def addClient(sessionId: String, userId: Int, pub: SourceQueue[String], ttl: Duration)(implicit ec: ExecutionContext, system: ActorSystem) = {
    sockets.update(sessionId, pub)
    userSessions.get(userId) match {
      case Some(value) => userSessions.update(userId, value ++ Set(sessionId))
      case None => userSessions.update(userId, Set(sessionId))
    }
    updateTTL(sessionId, ttl)
  }

  def removeClient(sessionId: String): Option[SourceQueue[String]] = sockets.remove(sessionId)
}
