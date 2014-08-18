package bug.persistencebug

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object Main {

  def main (args: Array[String]): Unit = {
    if (args.isEmpty || args.head == "Core")
      startRemoteSystem()
  }

  def startRemoteSystem() {
     val system = ActorSystem.create("Core", ConfigFactory.load("core"))
     val receptionActor = system.actorOf(ReceptionActor.props(), "reception")
     println("Successfully created reception actor: " + receptionActor.path.toString)
  }
}
