package com.optrak.experiment

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props}
import akka.persistence.ConfirmablePersistent
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import com.optrak.experiment.Controller.FireUpAkkaLand
import macroid.util.Ui
import macroid._
import macroid.FullDsl._

/**
 * Created by oscarvarto on 2014/06/13.
 */
object InitialAA {
  val BaseName = "InitialAA"
}

class InitialAA extends ThereActivity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    Log.d(TAG, "InitialAA#onCreate() called")
    super.onCreate(savedInstanceState)
    val view = l[LinearLayout] {
      f[InitialAAFragment].framed(Id.initial, Tag.initial)
    }
    setContentView(getUi(view))
  }
}

class InitialAAFragment extends InitialFragment with Contexts[Fragment] {
  val baseName: String = InitialAA.BaseName
  val actorName: String = Controller.Name

  def reactorProps(): Props = Props(new InitialReactor(this))
}

class InitialReactor(val owner: InitialAAFragment) extends Actor {

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    Log.d(TAG, "InitialReactor#preStart() called, before sendind FireUpAkkaLand")
    ActorSystemManager(owner.fragmentAppContext.get).controller ! FireUpAkkaLand
  }

  def receive: Receive = {
    case p @ ConfirmablePersistent(CreateThereActivity(clazz, _), _, _) =>
      Log.d(TAG, "InitialReactor received a CreateThereActivity msg")
      Ui {
        val i = new Intent(owner.getActivity, clazz)
        owner.startActivity(i)
        owner.getActivity.finish()
      }.run
      p.confirm()
  }
}

