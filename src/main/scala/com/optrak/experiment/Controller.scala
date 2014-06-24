package com.optrak.experiment

import akka.actor._
import akka.persistence.{Persistent, ConfirmablePersistent}
import android.util.Log
import com.optrak.experiment.Controller._
import com.optrak.experiment.LogonActivityActor.UserData

/**
 * Created by oscarvarto on 2014/06/11.
 */
object Controller {
  val Name = "Controller"

  sealed trait State
  case object Init extends State
  case object Logon extends State
  case object Mood extends State
  case object Tasks extends State

  trait ControllerPMsg extends PMsg {
    def destination: String = s"/user/${Controller.Name}"
  }

  // Messages
  case object FireUpAkkaLand
  case object StartSecondAA extends ControllerPMsg
  case object Quit
  case object Finish
  case object StartTaskListAA extends ControllerPMsg
  case object GoToLogonAA extends ControllerPMsg
  case object GoToPreviousAA extends ControllerPMsg

  def props(): Props = Props(new Controller)
}


class Controller extends FSM[State, List[ActorRef]] with ActorLogging {

  startWith(Init, List.empty)

  when(Init) {
    case Event(FireUpAkkaLand, emptyStack) =>
      val logonActivityActor = context.system.actorOf(LogonActivityActor.props(UserData()), LogonActivityActor.Name)
      goto(Logon) using List(logonActivityActor)
  }

  when(Logon) {
    case Event(p @ ConfirmablePersistent(StartSecondAA, _, _), activityStack) =>
      Log.d(TAG, "Controller (in Logon State) received message StartSecondAA")
      val secondActivityActor = context.system.actorOf(MoodActivityActor.props(), MoodActivityActor.Name)
      p.confirm()
      goto(Mood) using secondActivityActor :: activityStack
    case Event(Quit, activityStack) =>
      sender ! Finish
      killTopOfStack(activityStack) // Kill LogonActivityActor, the only activity in the stack
      goto(Init) using List.empty
  }

  when(Mood) {
    case Event(p @ ConfirmablePersistent(StartTaskListAA, _, _), activityStack) =>
      val tasksActivityActor = context.system.actorOf(TasksActivityActor.props(), TasksActivityActor.Name)
      p.confirm()
      goto(Tasks) using tasksActivityActor :: activityStack
    case Event(p @ ConfirmablePersistent(GoToPreviousAA, _, _), activityStack) =>
      ActorSystemManager.fsmProcessor ! Persistent(CloseThereActivity(moodReactor))
      val newStack = killTopOfStack(activityStack) // kill SecondActivityActor
      p.confirm()
      goto(Logon) using newStack
  }

  when(Tasks) {
    case Event(p @ ConfirmablePersistent(GoToLogonAA, _, _), activityStack) =>
      ActorSystemManager.fsmProcessor ! Persistent(CloseThereActivity(tasksReactor))
      val auxStack = killTopOfStack(activityStack) // kill tasksActivityActor
      ActorSystemManager.fsmProcessor ! Persistent(CloseThereActivity(moodReactor))
      val newStack = killTopOfStack(auxStack) // kill SecondActivityActor
      goto(Logon) using newStack
    case Event(p @ ConfirmablePersistent(GoToPreviousAA, _, _), activityStack) =>
      ActorSystemManager.fsmProcessor ! Persistent(CloseThereActivity(tasksReactor))
      val newStack = killTopOfStack(activityStack) // kill ThirdActivityActor
      goto(Mood) using newStack
  }


  /**
   * Pops top of `ActivityActor` stack and kills it.
   *
   * This method assumes the caller is not passing an empty `activityStack`.
   * In such a case the method throws a RunTimeException.
   *
   * @param activityStack
   * @return Updated activityStack
   */
  private def killTopOfStack(activityStack: List[ActorRef]): List[ActorRef] = {
    import scalaz.syntax.std.option._
    val errorMsg = "Activity Stack cannot be empty to be able to call killTopOfStack"
    val topActivity = activityStack.headOption.err(errorMsg)
    topActivity ! PoisonPill
    activityStack.tail
  }

  private def moodReactor = s"/user/${MoodAA.BaseName + "Reactor"}"
  private def tasksReactor = s"/user/${TaskListAA.BaseName + "ListReactor"}"

  initialize()
}