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

  // database configuration
  private val ordersDatabase = Paths.get(config.getString("orders-database")).toAbsolutePath
  private val booksDatabases = for (file <- config.getStringList("books-databases").asScala.toList) yield Paths.get(file).toAbsolutePath
  private val booksDirectory = Paths.get(config.getString("books-directory")).toAbsolutePath

  // actors
  private val orderActor = context.actorOf(OrderActor.props(ordersDatabase), "order")

  // handlers
  private def handleSearchRequest(request: SearchRequest): Unit = context.actorOf(SearchActor.props(request, context.sender(), booksDatabases), "search:" + request.uuid)
  private def handleOrderRequest(request: OrderRequest): Unit = orderActor.forward(request)
  private def handleReadRequest(request: ReadRequest): Unit = context.actorOf(ReadActor.props(request, context.sender(), booksDirectory), "read:" + request.uuid)

  // handling messages
  override def receive: Receive = {
    case request: SearchRequest => handleSearchRequest(request)
    case request: OrderRequest => handleOrderRequest(request)
    case request: ReadRequest => handleReadRequest(request)

    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")
}
