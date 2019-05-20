package pl.regzand.akkalibrary.server

import java.util.UUID
import java.nio.file.{Path, Paths}

import scala.collection.JavaConverters._
import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.Config
import pl.regzand.akkalibrary.messages.{OrderRequest, ReadRequest, SearchRequest}

object ServerActor {
  def props(config: Config): Props = Props(new ServerActor(config))
}

class ServerActor(val config: Config) extends Actor with ActorLogging {

  // actors configuration
  val ordersDatabase: Path = Paths.get(config.getString("orders-database")).toAbsolutePath
  val booksDatabases: List[Path] = for (file <- config.getStringList("books-databases").asScala.toList) yield Paths.get(file).toAbsolutePath
  val booksDirectory: Path = Paths.get(config.getString("books-directory")).toAbsolutePath

  /**
    * Handles search request by creating SearchActor
    */
  private def handleSearchRequest(request: SearchRequest) = {
    context.actorOf(
      SearchActor.props(request, context.sender(), booksDatabases),
      "search:" + UUID.randomUUID().toString
    )
  }

  /**
    * Handles order request by creating OrderActor
    */
  private def handleOrderRequest(request: OrderRequest) = {
    context.actorOf(
      OrderActor.props(request, context.sender(), ordersDatabase),
      "order:" + UUID.randomUUID().toString
    )
  }

  /**
    * Handles read request by creating ReadActor
    */
  private def handleReadRequest(request: ReadRequest) = {
    context.actorOf(
      ReadActor.props(request, context.sender(), booksDirectory),
      "read:" + UUID.randomUUID().toString
    )
  }

  override def receive: Receive = {
    case request: SearchRequest => handleSearchRequest(request);
    case request: OrderRequest => handleOrderRequest(request);
    case request: ReadRequest => handleReadRequest(request);
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")
}
