package pl.regzand.akkalibrary.client.commands

import akka.actor.ActorRef
import org.backuity.clist.{Command, arg}
import pl.regzand.akkalibrary.messages.OrderRequest

/**
  * Client CLI command that sends order request
  */
object OrderCommand extends Command("order", "order book from library") with BaseCommand {

  var title: String = arg[String](name = "title", description = "book to order")

  override def run(actor: ActorRef): Unit = actor ! OrderRequest(title)

}
