package pl.regzand.akkalibrary.server

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, Props}

object DatabaseSearchActor {
  def props(file: Path, title: String): Props = Props(new DatabaseSearchActor(file, title))
}

class DatabaseSearchActor(file: Path, title: String) extends Actor with ActorLogging {

  // not expecting any messages
  override def receive: Receive = {
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  //
}
