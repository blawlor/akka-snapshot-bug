package bug.persistencebug.simulator

import akka.actor._
import bug.persistencebug.ReceptionActor.{CreateLeaf, Initialize}
import com.github.nscala_time.time.Imports.DateTime

import scala.util.Random

object Simulator {

  sealed trait SimulatorMessage
  case object FindTimeout

  def props(replyTo: ActorRef, numberOfLeaves: Int) = Props(classOf[Simulator], replyTo, numberOfLeaves)

  val r  = Random

  def randomBetween(low: Int, high:Int): Int = {
    low + (r.nextInt(high-low+1).abs)
  }

  def randomFromList(listItems: List[String]): String = {
    val index = randomBetween(0, listItems.size - 1)
    listItems(index)
  }

  val exactLocations = List("europe/italy/sardinia/cagliari", "europe/italy/sardinia/villasimius", "europe/italy/sardinia/alghero", "europe/italy/sicily/tranpani")

}
class Simulator(replies: ActorRef, numberOfLeaves: Int) extends Actor with ActorLogging{
  import scala.concurrent.duration._
  import context.dispatcher
  import Simulator._

  def receive = startup

  log.debug("Sending Identify to Core reception")
  val receptionSelection = context.actorSelection("akka.tcp://Core@127.0.0.1:2552/user/reception")
  receptionSelection ! Identify("hello")

  val findTimeout = context.system.scheduler.scheduleOnce(5 seconds, self, FindTimeout)

  def startup: Receive = {
    case ActorIdentity(_, refOption) =>
      refOption match {
        case None => log.error("Can't find the reception actor remotely")
        case Some(ref) =>
          log.debug("Found the reception actor remotely")
          findTimeout.cancel()
          ref !  Initialize(replies: ActorRef)
          startSimulation(ref)
      }
    case FindTimeout =>
      log.error("Timed out waiting for reception actor")

    case whatever => log.warning("Missing out on message {}", whatever)
  }

 
  def startSimulation(tree: ActorRef) ={
    (1 to numberOfLeaves) foreach { index =>
      tree ! CreateLeaf(LeafImpl(index, randomFromList(Simulator.exactLocations), randomBetween(8000, 9000), randomBetween(1200, 1300), DateTime.now, DateTime.now.plusDays(7)))
    }
    
  }
}
