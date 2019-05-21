package pl.regzand.akkalibrary.server

import java.nio.file.{Files, NoSuchFileException, Path}

import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.stream.ActorMaterializer
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
  implicit val materializer: ActorMaterializer = ActorMaterializer.create(context.system)

  // generate file path
  private val path = booksDirectoryPath.resolve(request.title.toLowerCase.replace(" ", "-") + ".txt")

  if(Files.isRegularFile(path)) {

    // stream file
    FileIO.fromPath(path)
      .via(Framing.delimiter(ByteString("\n"), 256, allowTruncation = true).map(_.utf8String))
      .throttle(1, 1.second)
      .runWith(Sink.actorRef(client, request.successfulResponse))

  } else {
    client ! request.notFoundResponse
  }

  // kill itself after finishing streaming
  self ! PoisonPill

  // not expecting any messages
  override def receive: Receive = {
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // logging
  log.debug(self.path.name + " started")

}

object ReadActor {
  def props(request: ReadRequest, client: ActorRef, booksDirectoryPath: Path): Props = Props(new ReadActor(request, client, booksDirectoryPath))
}
