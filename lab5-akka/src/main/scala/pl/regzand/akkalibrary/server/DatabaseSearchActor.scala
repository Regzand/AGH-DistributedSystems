package pl.regzand.akkalibrary.server

import java.nio.file.Path

import scala.util.control.Breaks.{break, breakable}
import akka.actor.{Actor, ActorLogging, PoisonPill, Props}

import scala.io.Source

/**
  * Actor that searches given file for given title and returns result to its parent
  * @param file - file to search in
  * @param title - title to search for
  */
class DatabaseSearchActor(file: Path, title: String) extends Actor with ActorLogging {

  // not expecting any messages
  override def receive: Receive = {
    case msg => log.error("Received unexpected message: " + msg.toString)
  }

  // searching
  private val source = Source.fromFile(file.toFile)

  breakable {
    for (line <- source.getLines()) {
      val splited = line.split(",")

      if (splited(0).equalsIgnoreCase(title)) {
        context.parent ! splited(1).toFloat
        break
      }
    }
    context.parent ! None
  }

  source.close()

  // kill self
  self ! PoisonPill

}

object DatabaseSearchActor {
  def props(file: Path, title: String): Props = Props(new DatabaseSearchActor(file, title))
}
