package com.optrak.experiment

import akka.actor.Props
import akka.persistence.Persistent
import android.app.{Fragment, ListFragment}
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.LayoutParams._
import android.view.{LayoutInflater, Gravity, View, ViewGroup}
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget._
import com.optrak.experiment.TaskListAA.{A, S}
import com.optrak.experiment.TasksActivityActor.{AppendTask, RequestTaskList, TaskList, TaskModified}
import com.optrak.experiment.ThereActivityReactorTypes.{WaitingForInitialiser, Working}
import com.optrak.experiment.model.{Initialiser, Task}
import macroid.FullDsl._
import macroid._
import macroid.contrib.ExtraTweaks._
import macroid.util.Ui

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object TaskListAA {
  val BaseName = "TaskListAA"
  type S = TasksActivityActor.TasksActivityActorState
  type A = Unit
  val tasksR = new ThereActivityReactorTypes[S, A] {}
}

class TaskListAA extends ThereActivity {

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    val lps = lp[LinearLayout](MATCH_PARENT, WRAP_CONTENT, 1.0f) // layout params
    val view = l[LinearLayout](
        f[TaskListFragment].framed(Id.taskList, Tag.taskList) <~ lps,
        f[ButtonsFragment].framed(Id.buttons, Tag.buttons) <~ lps
      ) <~ vertical
    setContentView(getUi(view))
  }
}

/**
 * Created by oscarvarto on 2014/06/11.
 */
class TaskListFragment extends ListFragment with ThereFragment[TaskListAA.S, TaskListAA.A] with Contexts[ListFragment] with IdGeneration {
  val baseName: String = TaskListAA.BaseName + "List"
  var init: Option[Initialiser] = None

  def reactorProps(): Props = Props(new TasksReactor(this))
  val actorName: String = TasksActivityActor.Name

  var mTasks = ArrayBuffer.empty[Task]

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getActivity.setTitle(R.string.tasks_title)
    setListAdapter(new TaskAdapter())
  }

  def showNewTaskList(): Unit = {
    val adapter = getListAdapter.asInstanceOf[TaskAdapter]
    adapter.clear()
    adapter.addAll(mTasks)
    updateUI()
  }

  def updateUI() = getListAdapter.asInstanceOf[TaskAdapter].notifyDataSetChanged()

  def updateUserInterface(fsmState: TaskListAA.S, otherData: TaskListAA.A): Unit = {
    updateUI()
  }

  private class TaskAdapter(context: Context, resource: Int, objects: JList[Task]) extends
  ArrayAdapter[Task](context, resource, objects) {

    def this() = this(getActivity, 0, mTasks)

    def doneCheckBoxTweak(task: Task, position: Int) = Tweak[CheckBox] { checkbox =>

      checkbox.setGravity(Gravity.CENTER)
      checkbox.setFocusable(true)

      checkbox.setChecked(task.mDone) // <--- very important

      val rlp = checkbox.getLayoutParams.asInstanceOf[RelativeLayout.LayoutParams]
      rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
      checkbox.setLayoutParams(rlp)

      checkbox.setOnCheckedChangeListener {
        new OnCheckedChangeListener {
          def onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean): Unit = {
            task.mDone = isChecked
            reactor ! TaskModified(task, position)
          }
        }
      }

    }

    def taskTitleTweak = Tweak[TextView] { textview =>
      textview.setFocusable(false)
      val rlp = textview.getLayoutParams.asInstanceOf[RelativeLayout.LayoutParams]
      rlp.addRule(RelativeLayout.LEFT_OF, Id.done)
      textview.setLayoutParams(rlp)
    }

    override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
      val task: Task = getItem(position)
      val view = l[RelativeLayout](
        w[CheckBox] <~ id(Id.done) <~
          layoutParams[RelativeLayout](WRAP_CONTENT, WRAP_CONTENT) <~
          doneCheckBoxTweak(task, position) <~ enable(true) <~ padding(4 dp),
        w[TextView] <~ text(task.title) <~
          TextStyle.bold <~
          layoutParams[RelativeLayout](MATCH_PARENT, WRAP_CONTENT) <~
          taskTitleTweak
      )
      getUi(view)
    }
  }
}

class TasksReactor(owner: TaskListFragment) extends TaskListAA.tasksR.Reactor(owner, TasksActivityActor.Name) {
  when(Working) {
    gettingNewStateAndData orElse {
      case Event(modification: TaskModified, _) =>
        ActorSystemManager.fsmProcessor ! Persistent(modification)
        stay()
      case Event(tasks: TaskList, _) =>
        Ui {
          owner.mTasks = ArrayBuffer(tasks.value: _*)
          owner.showNewTaskList()
        }.run
        stay()
    }
  }

  onTransition {
    case WaitingForInitialiser -> Working =>
      activityActorSelection ! RequestTaskList
  }
}

class ButtonsFragment extends ThereFragment[TaskListAA.S, TaskListAA.A] with Contexts[Fragment] with IdGeneration {
  val baseName: String = TaskListAA.BaseName + "Buttons"
  val actorName: String = TasksActivityActor.Name
  def reactorProps(): Props = Props(new ButtonsReactor(this))

  var init: Option[Initialiser] = None


  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = getUi {
    l[LinearLayout] (
      w[Button] <~ text(R.string.append_task) <~
        On.click{ Ui {
          val newTask = Task(workday.mTasks.length)
          reactor ! AppendTask(newTask)
        }},
      w[Button] <~ text(R.string.go_to_previous_screen) <~ On.click { Ui(reactor ! Controller.GoToPreviousAA) },
      w[Button] <~ text(R.string.go_to_logon_screen) <~ On.click { Ui(reactor ! Controller.GoToLogonAA)},
      w[Button] <~ text(R.string.bomb) <~ bombTweak(TaskListAA.BaseName)
    )
  }

  def updateUserInterface(fsmState: S, otherData: A): Unit = {}
}

class ButtonsReactor(owner: ButtonsFragment) extends TaskListAA.tasksR.Reactor(owner, TasksActivityActor.Name) {
  when(Working) {
    case Event(msg: AppendTask, _) =>
      ActorSystemManager.fsmProcessor ! Persistent(msg)
      stay()
    case Event(msg @ Controller.GoToPreviousAA, _) =>
      ActorSystemManager.fsmProcessor ! Persistent(msg)
      stay()
    case Event(msg @ Controller.GoToLogonAA, _) =>
      ActorSystemManager.fsmProcessor ! Persistent(msg)
      stay()
  }
}