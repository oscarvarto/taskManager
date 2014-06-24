package com.optrak.experiment

import akka.actor.Props
import akka.persistence.Persistent
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.util.Log
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.{Button, LinearLayout}
import com.optrak.experiment.MoodAA.{A, S}
import com.optrak.experiment.MoodActivityActor.{Sad, MakeMeHappy, MakeMeSad, Happy}
import com.optrak.experiment.ThereActivityReactorTypes.{Working, RequestStateAndData}
import com.optrak.experiment.model.Initialiser
import macroid._
import macroid.FullDsl._
import macroid.util.Ui
import android.app.Fragment

/**
 * Created by oscarvarto on 2014/06/14.
 */
object MoodAA {
  val BaseName = "SecondAA"
  type S = MoodActivityActor.MoodActivityActorState
  type A = Unit
  val moodR = new ThereActivityReactorTypes[S, A] {}
}

class MoodAA extends ThereActivity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    val view = l[LinearLayout] {
      f[MoodFragment].framed(Id.mood, Tag.mood)
    }
    setContentView(getUi(view))
  }
}

class MoodFragment extends ThereFragment[MoodAA.S, MoodAA.A] with Contexts[Fragment] {
  val baseName: String = MoodAA.BaseName
  val actorName: String = MoodActivityActor.Name
  def reactorProps(): Props = Props(new MoodReactor(this))

  var init: Option[Initialiser] = None

  var changeMoodBtn = slot[Button]

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = getUi{
      l[LinearLayout](
        w[Button] <~ wire(changeMoodBtn),
        w[Button] <~ text(R.string.start_task_list) <~ On.click { Ui(reactor ! Controller.StartTaskListAA) },
        w[Button] <~ text(R.string.go_to_previous_screen) <~ On.click { Ui(reactor ! Controller.GoToPreviousAA) },
        w[Button] <~ text(R.string.bomb) <~ bombTweak(MoodAA.BaseName)
      ) <~ vertical
    }
    reactor ! RequestStateAndData
    view
  }

  def updateUserInterface(fsmState: S, otherData: A): Unit = {
    //Log.d(TAG, s"Calling SecondAA#updateUserInterface with fsmState: $fsmState")
    val (changeMoodLabel, msgToReactor) = fsmState match {
      case Happy => (R.string.make_me_sad, MakeMeSad)
      case Sad => (R.string.make_me_happy, MakeMeHappy)
    }
    changeMoodBtn foreach { btn =>
      btn.setText(changeMoodLabel)
      btn.setOnClickListener(new View.OnClickListener {
        def onClick(v: View): Unit = reactor ! msgToReactor
      })
    }
  }

}

class MoodReactor(owner: MoodFragment) extends MoodAA.moodR.Reactor(owner, MoodActivityActor.Name) {
  when(Working) {
    gettingNewStateAndData orElse {
      case Event(moodMsg @ MakeMeHappy, _) =>
        ActorSystemManager.fsmProcessor ! Persistent(moodMsg)
        stay()
      case Event(moodMsg @ MakeMeSad, _) =>
        ActorSystemManager.fsmProcessor ! Persistent(moodMsg)
        stay()
      case Event(msg @ Controller.StartTaskListAA, _) =>
        ActorSystemManager.fsmProcessor ! Persistent(msg)
        stay()
      case Event(msg @ Controller.GoToPreviousAA, _) =>
        ActorSystemManager.fsmProcessor ! Persistent(msg)
        stay()
    }
  }
}