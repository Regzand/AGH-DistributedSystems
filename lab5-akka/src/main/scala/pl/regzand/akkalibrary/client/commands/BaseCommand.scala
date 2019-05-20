package pl.regzand.akkalibrary.client.commands

import akka.actor.ActorRef

trait BaseCommand {
  def run(actor: ActorRef): Unit
}
