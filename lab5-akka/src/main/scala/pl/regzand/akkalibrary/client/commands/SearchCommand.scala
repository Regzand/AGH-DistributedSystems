package pl.regzand.akkalibrary.client.commands

import akka.actor.ActorRef
import org.backuity.clist.{Command, arg}
import pl.regzand.akkalibrary.messages.SearchRequest

/**
  * Client CLI command that sends search request
  */
object SearchCommand extends Command("search", "search for book in library") with BaseCommand {

  var title: String = arg[String](name = "title", description = "book to search for")

  override def run(actor: ActorRef): Unit = actor ! SearchRequest(title)

}
