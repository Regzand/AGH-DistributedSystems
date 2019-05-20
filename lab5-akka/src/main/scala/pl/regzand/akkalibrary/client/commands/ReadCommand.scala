package pl.regzand.akkalibrary.client.commands

import akka.actor.ActorRef
import org.backuity.clist.{Command, arg}
import pl.regzand.akkalibrary.messages.ReadRequest

object ReadCommand extends Command("read", "read book from library") with BaseCommand {

  var title: String = arg[String](name = "title", description = "book to read")

  override def run(actor: ActorRef): Unit = actor ! ReadRequest(title)

}
