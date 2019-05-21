package pl.regzand.akkalibrary.client

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.backuity.clist.Cli
import pl.regzand.akkalibrary.client.commands.{OrderCommand, ReadCommand, SearchCommand}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Entry point of client app
  */
object Client extends App {

  // load configuration
  val config = ConfigFactory.load("client")

  // create actor system
  val system = ActorSystem("client", config)

  // get remote actor
  val library = system.actorSelection(s"akka.tcp://library@${config.getString("library.host")}:${config.getInt("library.port")}/user/server")

  // create client actor
  val clientActor = system.actorOf(ClientActor.props(library), "client")

  // parse arguments and execute commands
  Cli.parse(args)
    .withCommands(SearchCommand, OrderCommand, ReadCommand)
    .foreach(_.run(clientActor))

  // wait for termination
  Await.result(system.whenTerminated, Duration.Inf)

}
