package bug.persistencebug

import akka.actor.{Props, ActorRef, ActorLogging, Actor}

object CollectionActor {
  case class RegisterListener(forwardTo: ActorRef)
  def props() = Props(classOf[CollectionActor])

}

/*
Funnels all outgoing messages to be sent back to client. E
 */
class CollectionActor extends Actor with ActorLogging{

  import CollectionActor._
  
  private var outstandingEvents: List[Any] = List.empty
  def receive = startup

  def startup: Receive = {
    case RegisterListener(listener) =>
      context.become(registered(listener))
      outstandingEvents foreach {event =>
        listener forward event
      }
    case unexpected => outstandingEvents = outstandingEvents :+ unexpected
  }

  def registered(listener: ActorRef): Receive = {

    case everything => listener forward everything

  }
  
}
