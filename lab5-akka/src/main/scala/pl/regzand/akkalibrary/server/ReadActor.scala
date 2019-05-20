package pl.regzand.akkalibrary.server

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.regzand.akkalibrary.messages.ReadRequest

object ReadActor {
  def props(request: ReadRequest, client: ActorRef, booksDirectoryPath: Path): Props = Props(new ReadActor(request, client, booksDirectoryPath))
}

class ReadActor(val request: ReadRequest, val client: ActorRef, val booksDirectoryPath: Path) extends Actor with ActorLogging {

  override def receive: Receive = {
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")

}
