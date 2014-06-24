package com.optrak.experiment

import akka.actor.{ActorSelection, FSM, Props}
import akka.persistence.{ConfirmablePersistent, Persistent}
import android.util.Log
import com.optrak.experiment.ThereActivityReactorTypes.{RequestStateAndData, RequestInitialisation}
import com.optrak.experiment.model.Initialiser

/**
 * Created by oscarvarto on 2014/06/14.
 */
object MoodActivityActor {
  def Name = "SecondActivityActor"
  def props(): Props = Props(new MoodActivityActor)

  sealed trait MoodActivityActorState
  case object Happy extends MoodActivityActorState
  case object Sad extends MoodActivityActorState

  // Inbound Messages
  trait SecondPMsg extends PMsg {
    def destination: String = s"/user/${MoodActivityActor.Name}"
  }
  case object MakeMeHappy extends SecondPMsg
  case object MakeMeSad extends SecondPMsg
}

import MoodActivityActor._
class MoodActivityActor extends FSM[MoodActivityActorState, Unit]{
  override def preStart() {
    val logonReactor = s"/user/${LogonAA.BaseName + "Reactor"}"
    ActorSystemManager.fsmProcessor ! Persistent(CreateThereActivity(classOf[MoodAA], logonReactor))
  }

  startWith(Happy, ())

  when(Happy) {
    case Event(p @ ConfirmablePersistent(MakeMeSad, _, _), _) =>
      p.confirm()
      goto(Sad)
  }

  when(Sad) {
    case Event(p @ ConfirmablePersistent(MakeMeHappy, _, _), _) =>
      p.confirm()
      goto(Happy)
  }

  whenUnhandled {
    case Event(RequestInitialisation, _) =>
      Log.d(TAG, s"SecondActivityActor received a RequestInitialisation from $sender")
      sender ! Initialiser()
      stay()
    case Event(RequestStateAndData, _) =>
      sender ! MoodAA.moodR.StateAndData(stateName, ())
      stay()
  }

  onTransition {
    case Happy -> Sad =>
      secondReactor ! MoodAA.moodR.StateAndData(Sad, ())
    case Sad -> Happy =>
      secondReactor ! MoodAA.moodR.StateAndData(Happy, ())
  }

  def secondReactor: ActorSelection = context.actorSelection(s"/user/${MoodAA.BaseName + "Reactor"}")

  initialize()
}
