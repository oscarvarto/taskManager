package com.optrak.experiment

/**
 * Created by oscarvarto on 2014/06/11.
 */

import akka.actor.{ActorSelection, FSM, Props}
import akka.persistence.{ConfirmablePersistent, Persistent}
import android.util.Log
import com.optrak.experiment.Controller.StartSecondAA
import com.optrak.experiment.ThereActivityReactorTypes.{RequestInitialisation, RequestStateAndData}
import com.optrak.experiment.model.Initialiser

object LogonActivityActor {
  val Name = "LogonActivityActor"

  def props(initialData: UserData): Props = Props(new LogonActivityActor(initialData))

  sealed trait LogonActivityActorState
  case object Initial extends LogonActivityActorState
  case object LoggedOn extends LogonActivityActorState

  // Data
  case class UserData(name: String = "", emailAddress: String = "")
  val noUserData = UserData()

  // Inbound message
  case class Logon(userData: UserData, destination: String = s"/user/${LogonActivityActor.Name}") extends PMsg
  // Information to restore LogonAA (outbound message)
  case class LogonStateAndData(fsmState: LogonActivityActorState, otherData: UserData)
  case class UpdateName(name: String)
  case class UpdateEmailAddress(emailAddress: String)
}

import LogonActivityActor._
class LogonActivityActor(val initialData: UserData) extends FSM[LogonActivityActorState, UserData] {
  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    Log.d(TAG, s"LogonActivityActor#preStart() called")
    val initialReactor = s"/user/${InitialAA.BaseName + "Reactor"}"
    ActorSystemManager.fsmProcessor ! Persistent(CreateThereActivity(classOf[LogonAA], initialReactor))
  }

  startWith(Initial, initialData)

  when(Initial) {
    case Event(RequestStateAndData, userData) =>
      //Log.d(TAG, s"RequestStateAndData received in $stateName, $stateData")
      sender ! LogonAA.logonR.StateAndData(Initial, userData)
      stay()
  }

  when(LoggedOn) {
    case Event(RequestStateAndData, userData) =>
      //Log.d(TAG, s"RequestStateAndData received in LoggedOn $stateName, $stateData")
      sender ! LogonAA.logonR.StateAndData(LoggedOn, userData)
      stay()
  }

  whenUnhandled {
    case Event(p @ ConfirmablePersistent(Logon(userData, _), _, _), _) =>
      //Log.d(TAG, s"LogonActivityActor received Logon($userData) in $stateName")
      ActorSystemManager.fsmProcessor ! Persistent(StartSecondAA)
      p.confirm()
      goto(LoggedOn) using userData
    case Event(RequestInitialisation, _) =>
      Log.d(TAG, s"LogonActivityActor received a RequestInitialisation from $sender")
      sender ! Initialiser()
      stay()
    case Event(UpdateName(aName), oldUserData) =>
      val newData = oldUserData.copy(name = aName)
      //Log.d(TAG, s"LogonActivityActor received an UpdateName($aName) in $stateName, $newData")
      stay() using newData
    case Event(UpdateEmailAddress(anEmailAddress), oldUserData) =>
      val newData = oldUserData.copy(emailAddress = anEmailAddress)
      //Log.d(TAG, s"LogonActivityActor received an UpdateEmailAddress($anEmailAddress) in $stateName, $newData")
      stay() using newData
  }


  onTransition {
    case Initial -> LoggedOn =>
      //Log.d(TAG, s"LogonActivityActor transitioned to LoggedOn")
      val logonReactor = context.actorSelection(s"/user/${LogonAA.BaseName + "Reactor"}")
      logonReactor ! LogonAA.logonR.StateAndData(LoggedOn, nextStateData)
  }

  initialize()
}
