package bug.persistencebug.simulator

import akka.actor.ActorSystem
import bug.persistencebug.CollectionActor
import bug.persistencebug.CollectionActor.RegisterListener
import com.typesafe.config.ConfigFactory


object SimulatorMain  {

  implicit lazy val system = ActorSystem.create("Simulator", ConfigFactory.load("simulator"))

  def main (args: Array[String]): Unit = {
      startUserSimulator()
  }

  def startUserSimulator(){
    // A local actor to collect all the 'output' messages.
    val collector = system.actorOf(CollectionActor.props(), "collector")
    // The reception gets deployed remotely as seen in the property.conf

    val simulator = system.actorOf(Simulator.props(collector, 10))
    collector ! RegisterListener(simulator)


  }
}
