package pl.regzand.akkalibrary.server

import java.io.FileNotFoundException
import java.nio.file.Path

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import pl.regzand.akkalibrary.messages.SearchRequest

/**
  * Actor created for each search request, looks for given title in all available databases and returns result back to client
  * @param request - request that triggered creation of this client
  * @param client - client that has send request
  * @param booksDatabasesPaths - list of paths to files containing prices of books
  */
class SearchActor(val request: SearchRequest, val client: ActorRef, val booksDatabasesPaths: List[Path]) extends Actor with ActorLogging {

  // handle errors
//  override val supervisorStrategy: OneForOneStrategy =
//    OneForOneStrategy(maxNrOfRetries = -1, withinTimeRange = Duration.Inf, loggingEnabled = false) {
//      case _: FileNotFoundException => {
//        println("ee")
//        Restart
//      }
//      case _: Exception => Escalate
//    }

  private var nones = 0

  // handle messages
  override def receive: Receive = {
    case price: Float =>
      client ! request.priceResponse(price)
      context.stop(self)

    case None =>
      nones += 1
      if(nones >= booksDatabasesPaths.size){
        client ! request.notFoundResponse
        context.stop(self)
      }

    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // start searching
  for (path <- booksDatabasesPaths) {
    val supervisor = BackoffSupervisor.props(
      BackoffOpts
        .onFailure(
          DatabaseSearchActor.props(path, request.title),
          childName = "dbSearch:" + path.getFileName,
          minBackoff = 3.seconds,
          maxBackoff = 30.seconds,
          randomFactor = 0.2
        )
        .withSupervisorStrategy(OneForOneStrategy(loggingEnabled = false) {
          case _: FileNotFoundException => SupervisorStrategy.Restart
          case _ => SupervisorStrategy.Escalate
        })
    )

    context.actorOf(supervisor, "supervisor:" + path.getFileName)
  }

  // logging
  log.debug(self.path.name + " started")

}

object SearchActor {
  def props(request: SearchRequest, client: ActorRef, booksDatabasesPaths: List[Path]): Props = Props(new SearchActor(request, client, booksDatabasesPaths))
}
