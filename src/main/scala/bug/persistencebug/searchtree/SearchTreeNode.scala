package bug.persistencebug.searchtree

import akka.actor.SupervisorStrategy.{Stop, Escalate, Restart, Resume}
import akka.actor._
import akka.persistence.{Recover, SnapshotOffer, PersistentActor}
import bug.persistencebug.LeafActor
import bug.persistencebug.searchtree.Tree.PlaceLeaf

object SearchTreeNode {
  case object NodeIsEmpty
  def props(name: String, collector: ActorRef) = Props(classOf[SearchTreeNode], name, collector)

  case class State(val name: String, val collector: ActorRef,
                   childNodes: Seq[String] = Seq.empty,
                   childLeaves: Map[Long, String] = Map.empty) {
    def updateChildNodes(childNodes: Seq[String]) = copy(childNodes = childNodes)
    def updateChildLeaves(childLeaves: Map[Long, String]) = copy(childLeaves = childLeaves)
  }

}

class SearchTreeNode(val name: String, val collector: ActorRef) extends PersistentActor with ActorLogging {

  import scala.concurrent.duration._

  import SearchTreeNode.State
  def receiveCommand = running

  var state = State(name, collector)

  // Transient data, constructed over time or from state
  var childNodes: Map[String, ActorRef] = Map.empty
  var childLeaves: Map[Long, ActorRef] = Map.empty
  var childLeavesReverse: Map[ActorRef, Long] = Map.empty
  var hasRecovered: Boolean = false

//  Example of supervisor strategy.
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 2, withinTimeRange = 1 minute) {
      case e: ArithmeticException      => log.error(e, "Reacting to an ArithmeticException"); Escalate
      case e: NullPointerException     => log.error(e, "NPE");Escalate
      case e: IllegalArgumentException => log.error(e, "IllegalArgument");Escalate
      case e: Exception                => log.error(e, "Exception");Escalate
    }

  override def persistenceId = name

  log.warning("Starting up a tree node actor with persistence id {}", persistenceId)

  override def preStart() {
    log.warning("***Pre-Start of a tree node actor with persistence id {}", persistenceId)
    self ! Recover()
  }

  def running: Receive = {

    case PlaceLeaf(sequenceNumber, List(), leaf) =>
      val leafName = "lot"+leaf.id
      log.warning("Creating a LeafActor for leaf %s with name %s " .format(leaf.id, leafName))
      val leafActor = context.actorOf(LeafActor.props(leafName, leaf), leafName)
      context.watch(leafActor)
      childLeaves = childLeaves + (leaf.id -> leafActor)
      state = state.updateChildLeaves(state.childLeaves + (leaf.id -> leafName))
      childLeavesReverse = childLeavesReverse + (leafActor -> leaf.id)

    case PlaceLeaf(sequenceNumber, nodeName :: remainingPath, lot) =>
      if (!childNodes.contains(nodeName)) {
        val newChildName = name + "-" + nodeName
        val childNode = context.actorOf(SearchTreeNode.props(newChildName, collector), nodeName)
        childNodes = childNodes + (nodeName -> childNode)
        state = state.updateChildNodes(state.childNodes :+ newChildName)
        context.watch(childNode)
      }
      val nextMatchingNode = childNodes(nodeName)
      log.warning("Passing a PlaceLeaf for leaf %s down to the next matching node %s (%s) with remaining path = %s" .format(lot.id, nodeName, nextMatchingNode, remainingPath))
      nextMatchingNode ! new PlaceLeaf(sequenceNumber, remainingPath, lot)

    case "snap"  => saveSnapshot(state)
      childNodes.values foreach { node =>
        node ! "snap"
      }
      childLeaves.values foreach { leaf =>
        leaf ! "snap"
      }

    case whatever => log.warning("Missing a message: " + whatever)
  }

  val receiveRecover: Receive = {
    // reconstruct the persistent state and the cached maps based on the snapshot.
    case SnapshotOffer(metadata, snapshot: SearchTreeNode.State) =>
      state = snapshot
      log.warning("Reconstructing tree node {} (recovered? {}) from snapshot using metadata {} from actor with persistenceId = {}", state.name, hasRecovered, metadata, persistenceId)
      constructTransientDataFromState(state)
      hasRecovered = true
  }

  def constructTransientDataFromState(state: SearchTreeNode.State) = {
    childNodes = state.childNodes.map { nodeName =>
      nodeName -> context.actorOf(SearchTreeNode.props(nodeName, collector), nodeNameFromPath(nodeName))
    }.toMap

    childLeaves = state.childLeaves.map {
      case (key, value) =>
        val leafId = key
        val leafActorName = value
        log.warning("Recreating lot called {}", leafActorName)
        val leafActor = context.actorOf(LeafActor.props(leafActorName), leafActorName)
        leafId -> leafActor
    }
    childLeavesReverse = childLeaves.map(_.swap)
  }

  def nodeNameFromPath(path: String) :String = {
    path.split("/").reverse.head
  }
}
