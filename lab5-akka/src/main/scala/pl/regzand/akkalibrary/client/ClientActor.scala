package pl.regzand.akkalibrary.client

import akka.actor.{Actor, ActorLogging, ActorSelection, Props}
import pl.regzand.akkalibrary.messages.{NotFoundResponse, PriceResponse, Request, SearchRequest, SuccessfulResponse}

/**
  * Actor responsible for sending requests and printing its results
  * @param library - remote actor to which requests will be sent
  */
class ClientActor(library: ActorSelection) extends Actor with ActorLogging {

  override def receive: Receive = {
    case request: Request => library ! request

    case _: NotFoundResponse =>
      println("Not found")
      context.system.terminate()

    case _: SuccessfulResponse =>
      println("Successful")
      context.system.terminate()

    case response: PriceResponse =>
      println(s"Price: ${response.price}")
      context.system.terminate()

    case msg: String =>
      println(msg)

    case msg => log.error("Received unexpected message: " + msg.toString)
  }

}

object ClientActor {
  def props(library: ActorSelection): Props = Props(new ClientActor(library))
}
