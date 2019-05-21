package pl.regzand.akkalibrary.client.commands

import akka.actor.ActorRef

/**
  * Base for all client CLI commands
  */
trait BaseCommand {
  def run(actor: ActorRef): Unit
}
