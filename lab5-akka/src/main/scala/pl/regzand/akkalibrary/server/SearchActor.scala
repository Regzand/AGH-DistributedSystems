package pl.regzand.akkalibrary.server

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.regzand.akkalibrary.messages.SearchRequest

/**
  * Actor created for each search request, looks for given title in all available databases and returns result back to client
  * @param request - request that triggered creation of this client
  * @param client - client that has send request
  * @param booksDatabasesPaths - list of paths to files containing prices of books
  */
class SearchActor(val request: SearchRequest, val client: ActorRef, val booksDatabasesPaths: List[Path]) extends Actor with ActorLogging {

  override def receive: Receive = {
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")

}

object SearchActor {
  def props(request: SearchRequest, client: ActorRef, booksDatabasesPaths: List[Path]): Props = Props(new SearchActor(request, client, booksDatabasesPaths))
}
