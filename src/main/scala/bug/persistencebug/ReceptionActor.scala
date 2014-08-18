package bug.persistencebug;

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import akka.persistence.{SnapshotOffer, PersistentActor}
import bug.persistencebug.searchtree.Tree
import bug.persistencebug.searchtree.Tree.{PlaceLeaf}


object ReceptionActor {

  case class SystemState(sequenceNumber: Long = 1L, collector: ActorRef = null, searchTree: ActorRef = null) {
    def updated(newSequenceNumber: Long): SystemState = copy(sequenceNumber = newSequenceNumber)
    def updated(newCollector: ActorRef, searchTree: ActorRef): SystemState =
      copy(collector = newCollector, searchTree = searchTree)
    def updated(searchTree: ActorRef): SystemState = copy(searchTree = searchTree)
  }

  sealed trait Command
  case class Initialize(collector: ActorRef) extends Command
  case class CreateLeaf(leaf: Leaf) extends Command

  case class InitEvent(collector: ActorRef)
  def props() = Props(classOf[ReceptionActor])
}

import ReceptionActor._

class ReceptionActor() extends PersistentActor with ActorLogging {
  import scala.concurrent.duration._

  override def persistenceId = "reception"

  def receiveCommand = startup

  var state = SystemState()

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 2, withinTimeRange = 1 minute) {
      case e: ArithmeticException      => log.error(e, "Reacting to an ArithmeticException"); Escalate
      case e: NullPointerException     => log.error(e, "NPE");Escalate
      case e: IllegalArgumentException => log.error(e, "IllegalArgument");Escalate
      case e: Exception                => log.error(e, "Exception");Escalate
    }

  def startup: Receive = {
    case Initialize(collector) =>
      persist(InitEvent(collector)) { event =>
        log.debug("Incoming - Init Event: " + event)
        val searchTree = context.actorOf(Tree.props(collector), "searchtree")
        state = state.updated(collector, searchTree)
        context.become(running)
      }
  }

  def running: Receive = {

    case CreateLeaf(leaf) =>
      if (state.sequenceNumber %10 == 0 && state.sequenceNumber > 0) self ! "snap"
      persist(PlaceLeaf(state.sequenceNumber, leaf.searchElementList, leaf)) { event =>
        log.warning("Incoming - Create Leaf: " + leaf)
        state = state.updated(state.sequenceNumber+1)
        state.searchTree ! event
      }
    case "snap"  => saveSnapshot(state)
      if (state.searchTree != null) state.searchTree ! "snap"
    case "print" => println(state)
  }


  val receiveRecover: Receive = {
    case init: InitEvent =>
      log.warning("Recovering the Init event")
      val searchTree = context.actorOf(Tree.props(init.collector), "searchtree")
      state = state.updated(init.collector, searchTree)
      context.become(running)
    case placeLeaf: PlaceLeaf => state = state.updated(placeLeaf.sequenceNumber)
      state.searchTree ! placeLeaf
      log.warning("Recovering a PlaceLeaf event. Sequence = {}", placeLeaf.sequenceNumber)
    case SnapshotOffer(_, snapshot: SystemState) =>
      state = snapshot
      //TODO Recover actor references from state
      log.warning("Recovering snapshot. Last sequence was: " + state.sequenceNumber)
      val searchTree = context.actorOf(Tree.props(state.collector), "searchtree")
      state = state.updated(searchTree)
      context.become(running)
  }
}

