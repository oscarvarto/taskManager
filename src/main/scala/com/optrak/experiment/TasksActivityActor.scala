package com.optrak.experiment

import akka.actor.{ActorSelection, FSM, Props}
import akka.persistence.{ConfirmablePersistent, Persistent}
import android.util.Log
import com.optrak.experiment.TasksActivityActor.AppendTask
import com.optrak.experiment.ThereActivityReactorTypes.RequestInitialisation
import com.optrak.experiment.model.{Initialiser, Task}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by oscarvarto on 2014/06/14.
 */
object TasksActivityActor {
  val Name = "TasksActivityActor"

  def props(): Props = Props(new TasksActivityActor(workday.getTasks()))

  trait TasksPMsg extends PMsg {
    def destination: String = s"/user/${TasksActivityActor.Name}"
  }
  // Messages
  case object RequestTaskList
  case class TaskList(value: Vector[Task])
  case class AppendTask(task: Task) extends TasksPMsg
  case class TaskModified(newValue: Task, taskIndex: Int) extends TasksPMsg

  // States
  sealed trait TasksActivityActorState
  case object Initial extends TasksActivityActorState
  case object ShowingList extends TasksActivityActorState
}

import TasksActivityActor._
// TasksActivityActor has communication with several reactor in TaskListAA
// The one corresponding the task list
// Another that reacts to button presses ("Go back to LogonAA" & "Go to previous screen")
class TasksActivityActor(val tasks: Vector[Task]) extends FSM[TasksActivityActorState, Unit] {

  var backupTaskList: Vector[Task] = tasks

  def taskListReactor: ActorSelection = context.actorSelection(s"/user/${TaskListAA.BaseName + "ListReactor"}")

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val secondReactor = s"/user/${MoodAA.BaseName + "Reactor"}"
    ActorSystemManager.fsmProcessor ! Persistent(CreateThereActivity(classOf[TaskListAA], secondReactor))
  }

  startWith(Initial, ())

  when(Initial) {
    case Event(RequestTaskList, _) =>
      //Log.d(TAG, s"----->> TasksActivityActor received a RequestTaskList from $sender")
      sender ! TaskList(backupTaskList)
      goto(ShowingList)
  }

  when(ShowingList) {
    case Event(p @ ConfirmablePersistent(TaskModified(task, position), _, _), _) =>
      backupTaskList = tasks.updated(position, task)
      sender ! TaskListAA.tasksR.StateAndData(stateName, ())
      p.confirm()
      stay()
  }

  whenUnhandled {
    case Event(RequestInitialisation, _) =>
      sender ! Initialiser()
      if (sender.path.toString.endsWith("ListReactor")) goto(Initial) else stay()
    case Event(p @ ConfirmablePersistent(AppendTask(task), _, _), _) =>
      backupTaskList = backupTaskList :+ task
      taskListReactor ! TaskList(backupTaskList)
      p.confirm()
      stay()
  }

  override def postStop(): Unit = {
    workday.mTasks = backupTaskList
    super.postStop()
  }

  initialize()
}
