package bug.persistencebug

import akka.actor._
import akka.persistence.{PersistentActor, SnapshotOffer}

object LeafActor {
  def props(name: String, leaf: Leaf) = Props(classOf[LeafActor],  name, Some(leaf))
  def props(name: String) = Props(classOf[LeafActor], name, None)

  // State Data
  sealed trait StateData
  case class UninitializedData() extends StateData
  case class WaitingData(leaf: Leaf) extends StateData
}

import bug.persistencebug.LeafActor._

class LeafActor(val name: String, val initialLeafOption: Option[Leaf]) extends PersistentActor with ActorLogging{

  override def persistenceId = name

  log.warning("Starting up a lot actor with persistence id {}", persistenceId)

  def receiveCommand = uninitialized(UninitializedData())


  initialLeafOption match {
    case Some(leaf) =>
      log.warning("Lot Actor {} created with initial Lot: {}", leaf.id, leaf)
      context.become(waitOnMatchingSearches(WaitingData(leaf)))
    case None =>
      context.become(uninitialized(UninitializedData()))
  }

  def uninitialized(state: UninitializedData):Receive = {
    case "snap"  => saveSnapshot(state)

    case whatever => // Do nothing
  }


  def waitOnMatchingSearches(state: WaitingData):Receive = {
    case "snap"  => saveSnapshot(state)

    case whatever => log.warning("Lot Actor missing message {}", whatever)
  }

  val receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: StateData) =>
      log.warning("Recovering Leaf using snapshot")
      snapshot match {
        case s@UninitializedData() => context.become(uninitialized(s))
        case s@WaitingData(_) =>
          log.warning("Leaf {} is Waiting", s.leaf.id)
          context.become(waitOnMatchingSearches(s))
      }
  }


  def finish(leaf: Leaf) = {
    self ! PoisonPill
  }
}
