package pl.regzand.akkalibrary.server

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.regzand.akkalibrary.messages.OrderRequest

object OrderActor {
  def props(request: OrderRequest, client: ActorRef, ordersDatabasePath: Path): Props = Props(new OrderActor(request, client, ordersDatabasePath))
}

class OrderActor(val request: OrderRequest, val client: ActorRef, val ordersDatabasePath: Path) extends Actor with ActorLogging {

  override def receive: Receive = {
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")

}
