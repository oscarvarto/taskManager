package com.optrak.experiment

import akka.actor.FSM.Event
import akka.actor.{ActorSelection, FSM}
import akka.persistence.ConfirmablePersistent
import android.util.Log
import com.optrak.experiment.model.Initialiser
import android.content.Intent
import scalaz.syntax.std.option._
import scala.reflect.ClassTag
import macroid.util.Ui

/**
 * Created by oscarvarto on 2014/06/11.
 */
object ThereActivityReactorTypes {
  trait ReactorState
  case object WaitingForInitialiser extends ReactorState
  case object Working extends ReactorState

  case object RequestInitialisation
  case object RequestStateAndData
}

trait ThereActivityReactorTypes[S, A] {

  import ThereActivityReactorTypes._

  case class StateAndData(fsmState: S, otherData: A)

  trait AuxiliaryStateFunctions {
    self: Reactor =>

    val lifeCycleBehaviour: StateFunction = {
      case Event(p @ ConfirmablePersistent(CreateThereActivity(clazz, _), _, _), _) =>
        //Log.d(TAG, "Reactor received a CreateThereActivity message")
          Ui {
            val i = new Intent(owner.getActivity, clazz)
            owner.startActivity(i)
          }.run
        p.confirm()
        stay()
      case Event(p @ ConfirmablePersistent(CloseThereActivity(_), _, _), _) =>
        //Log.d(TAG, "Reactor received a CloseThereActivity message")
        Ui( owner.getActivity.finish() ).run
        p.confirm()
        stop()
    }

    val gettingNewStateAndData: StateFunction = {
      case Event(StateAndData(state, otherD), rData) =>
        //Log.d(TAG, s"${this.self} received a StateAndData($state, $otherD) from $sender")
        Ui( owner.updateUserInterface(state, otherD) ).run
        stay()
    }
  }

  abstract class Reactor(val owner: ThereFragment[S, A], val activityActorName: String) extends FSM[ReactorState, Unit]
    with AuxiliaryStateFunctions {

    def controller: ActorSelection = context.actorSelection(s"/user/${Controller.Name}")

    def activityActorSelection: ActorSelection = context.actorSelection(s"/user/$activityActorName")

    override def preStart() {
      Log.d(TAG, s"$self#preStart() called")
      activityActorSelection ! RequestInitialisation
    }

    startWith(WaitingForInitialiser, ())

    when(WaitingForInitialiser) {
      case Event(init: Initialiser, _) =>
        //Log.d(TAG, s"Reactor $self received an Initialiser from $sender")
        Ui( owner.init = init.some ).run
        goto(Working)
    }

    whenUnhandled {
      lifeCycleBehaviour orElse {
        case Event(msg, _) =>
          //Log.d(TAG, s"$self received $msg, resending to $activityActorName")
          activityActorSelection ! msg
          stay()
      }
    }

    initialize()
  }
}
