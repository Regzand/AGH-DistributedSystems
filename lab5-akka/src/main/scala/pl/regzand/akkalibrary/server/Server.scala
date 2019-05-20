package pl.regzand.akkalibrary.server

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Server extends App {
  // load configuration
  val config = ConfigFactory.load("server")

  // create actor system
  val system = ActorSystem("library", config)

  // create main actor
  val serverActor = system.actorOf(ServerActor.props(config.getConfig("library")), "server-actor")

  // wait for termination
  Await.result(system.whenTerminated, Duration.Inf)
}
