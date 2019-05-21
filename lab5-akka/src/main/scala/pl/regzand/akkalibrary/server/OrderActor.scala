package pl.regzand.akkalibrary.server

import java.nio.file.{Files, Path, StandardOpenOption}

import akka.actor.{Actor, ActorLogging, Props}
import pl.regzand.akkalibrary.messages.OrderRequest

/**
  * Actor responsible for handling all order requests
  * @param ordersDatabasePath - path to file to which orders will be saved
  */
class OrderActor(val ordersDatabasePath: Path) extends Actor with ActorLogging {

  // order request handler
  private def handleOrderRequest(request: OrderRequest): Unit = {

    // append to file
    Files.write(
      ordersDatabasePath,
      (request.title + "\n").getBytes,
      StandardOpenOption.CREATE, StandardOpenOption.APPEND
    )

    // send response
    context.sender() ! request.successfulResponse

  }

  // handling messages
  override def receive: Receive = {
    case request: OrderRequest => handleOrderRequest(request)
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")

}

object OrderActor {
  def props(ordersDatabasePath: Path): Props = Props(new OrderActor(ordersDatabasePath))
}
