package com.optrak.experiment

import android.app.{Fragment, Activity}
import android.os.Bundle
import android.util.Log
import macroid.{IdGeneration, Contexts}
import com.optrak.experiment.model.Initialiser
import akka.actor._

/**
 * Created by oscarvarto on 2014/06/11.
 */
/**
 * Message ordering the current reactor to (indirectly) create an instance of `ThereActivity`
 *
 * @param clazz Corresponds to the Android activity to be created
 */
case class CreateThereActivity[T <: ThereActivity](clazz: Class[T], destination: String) extends PMsg

/** Signals this reactor to close its owner */
case class CloseThereActivity(destination: String) extends PMsg

trait ThereActivity extends Activity with Contexts[Activity] with IdGeneration {
  //override def onBackPressed() {}
}

trait InitialFragment extends Fragment {
  val baseName: String

  def actorSystem: ActorSystem = ActorSystemManager.actorSys

  lazy val reactorName: String = baseName + "Reactor"
  def reactorProps(): Props
  var reactorOpt: Option[ActorRef] = None
  import scalaz.syntax.std.option._
  def reactor: ActorRef = reactorOpt.err(s"Reactor $reactorName should be Some(ActorRef) when calling Option#err")

  val actorName: String

  override def onCreate(savedInstanceState: Bundle): Unit = {
    reactorOpt = Some(ActorSystemManager(getActivity).actorSys.actorOf(reactorProps(), reactorName))
    Log.d(TAG, s"Fragment $this and $reactor CREATED")
    super.onCreate(savedInstanceState)
  }

  override def onDestroy(): Unit = {
    reactorOpt foreach {_ ! PoisonPill }
    Log.d(TAG, s"Fragment $this and $reactor DESTROYED")
    reactorOpt = None
    super.onDestroy()
  }
}

trait ThereFragment[S, A] extends InitialFragment  {
  var init: Option[Initialiser]

  def updateUserInterface(fsmState: S, otherData: A): Unit
}
