package bug.persistencebug.searchtree

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import bug.persistencebug.searchtree.Tree.PlaceLeaf
import bug.persistencebug.Leaf

object Tree {
  case class PlaceLeaf(sequenceNumber: Long, path:List[String], lot: Leaf)

  def props(collector: ActorRef) = Props(classOf[Tree], collector)

}

class Tree(collector: ActorRef) extends Actor with ActorLogging {
  import scala.concurrent.duration._

  val root:ActorRef = context.actorOf(SearchTreeNode.props("root", collector), name = "root")

  def receive = startup

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 2, withinTimeRange = 1 minute) {
      case e: ArithmeticException      => log.error(e, "Reacting to an ArithmeticException"); Escalate
      case e: NullPointerException     => log.error(e, "NPE");Escalate
      case e: IllegalArgumentException => log.error(e, "IllegalArgument");Escalate
      case e: Exception                => log.error(e, "Exception");Escalate
    }

  def startup:Receive = {
    case placeLot@PlaceLeaf(_,_,_) =>
      root ! placeLot
    case "snap" =>
      root ! "snap"
  }

}
