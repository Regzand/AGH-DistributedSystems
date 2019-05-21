package pl.regzand.akkalibrary.server

import java.nio.file.{Files, Path, Paths}

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString
import pl.regzand.akkalibrary.messages.ReadRequest

/**
  * Actor created for each read request, streams content of a book back to sender
  * @param request - read request that triggered creation of this actor
  * @param client - client that has send request
  * @param booksDirectoryPath - path to directory in which books are stored
  */
class ReadActor(val request: ReadRequest, val client: ActorRef, val booksDirectoryPath: Path) extends Actor with ActorLogging {

  // streams book back to sender
  private def streamBook():Unit = {
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    // generate file path
    val path = booksDirectoryPath.resolve(request.title.toLowerCase.replace(" ", "-") + ".txt")

    // check if file exists
    if(!Files.isRegularFile(path)) {
      client ! request.notFoundResponse
      context.stop(self)
      return
    }

    // stream file
    FileIO.fromPath(path)
      .via(Framing.delimiter(ByteString("\n"), 256, true).map(_.utf8String))
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
      .runWith(Sink.actorRef(client, request.successfulResponse))

    // kill itself
    context.stop(self)
  }

  // not expecting any messages
  override def receive: Receive = {
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")

  // start streaming
  streamBook()
}

object ReadActor {
  def props(request: ReadRequest, client: ActorRef, booksDirectoryPath: Path): Props = Props(new ReadActor(request, client, booksDirectoryPath))
}
