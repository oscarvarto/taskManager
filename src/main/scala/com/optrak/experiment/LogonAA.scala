package com.optrak.experiment

import akka.actor.{ActorSelection, Props}
import akka.persistence.Persistent
import android.app.Fragment
import com.optrak.experiment.ThereActivityReactorTypes.{RequestStateAndData, Working}
import android.os.Bundle
import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts.VerticalLinearLayout
import com.optrak.experiment.model.Initialiser
import android.widget.{Button, TextView, EditText}
import android.view.{View, ViewGroup, LayoutInflater}
import android.text.{Editable, TextWatcher, InputType}
import com.optrak.experiment.LogonActivityActor.{Logon, UserData, UpdateEmailAddress, UpdateName}
import macroid.util.Ui
import android.util.Log
import scalaz.syntax.std.option._

/**
 * Created by oscarvarto on 2014/06/11.
 */
object LogonAA {
  val BaseName = "LogonAA"
  type S = LogonActivityActor.LogonActivityActorState
  type A = LogonActivityActor.UserData
  val logonR = new ThereActivityReactorTypes[S, A] {}
}

class LogonAA extends ThereActivity {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    val view = l[VerticalLinearLayout](
      f[LogonFragment].framed(Id.logon, Tag.logon)
    )
    setContentView(getUi(view))
    // val reactor = ActorSystemManager.actorSys.actorSelection(s"/user/${LogonAA.BaseName + "Reactor"}")
    // reactor ! RequestStateAndData
  }
}

class LogonFragment extends ThereFragment[LogonAA.S, LogonAA.A] with Contexts[Fragment] {
  val baseName = LogonAA.BaseName
  val actorName = LogonActivityActor.Name
  def reactorProps(): Props = Props(new LogonReactor(this))

  var init: Option[Initialiser] = None

  var userNameET = slot[EditText]
  var emailAddressET = slot[EditText]
  var logonActivityActorStateTV = slot[TextView]

  lazy val userNameTweak = Tweak[EditText] { et =>
    et.setHint(R.string.user_name)
    et.setInputType(InputType.TYPE_CLASS_TEXT)
    et.addTextChangedListener(new TextWatcher {
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {}
      def afterTextChanged(name: Editable): Unit = reactor ! UpdateName(name.toString)
    })
  }

  lazy val emailAddressTweak = Tweak[EditText] { et =>
    et.setHint(R.string.email_address_hint)
    et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
    et.addTextChangedListener(new TextWatcher {
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {}
      def afterTextChanged(emailAddress: Editable): Unit = reactor ! UpdateEmailAddress(emailAddress.toString)
    })
  }

  lazy val logonTweak = On.click {
    Ui {
      val f = { et: Option[EditText] => et.cata(_.getText.toString, "") }
      val name = f(userNameET)
      val emailAddress = f(emailAddressET)
      reactor ! Logon(UserData(name, emailAddress))
    }
  }

  lazy val quitTweak = On.click { Ui(reactor ! Controller.Quit) }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = getUi {
      l[VerticalLinearLayout](
        w[EditText] <~ wire(userNameET) <~ userNameTweak,
        w[EditText] <~ wire(emailAddressET) <~ emailAddressTweak,
        w[TextView] <~ wire(logonActivityActorStateTV),
        w[Button] <~ text(R.string.logon) <~ logonTweak,
        w[Button] <~ text(R.string.quit) <~ quitTweak,
        w[Button] <~ text(R.string.bomb) <~ bombTweak(LogonAA.BaseName)
      )
    }
    reactor ! RequestStateAndData
    view
  }

  def updateUserInterface(fsmState: LogonAA.S, userData: LogonAA.A): Unit = {
    //Log.d(TAG, s"fsmState: $fsmState, userData: $userData")
    val errorMsg = UserInterfaceNotReady + "inside LogonFragment"
    userNameET.err(errorMsg) setText userData.name
    emailAddressET.err(errorMsg) setText userData.emailAddress
    logonActivityActorStateTV.err(errorMsg) setText fsmState.toString
  }
}

class LogonReactor(owner: LogonFragment) extends LogonAA.logonR.Reactor(owner, LogonActivityActor.Name) {
  when(Working) {
    gettingNewStateAndData orElse {
      case Event(logonMsg: Logon, _) =>
        ActorSystemManager.fsmProcessor ! Persistent(logonMsg)
        stay()
      case Event(Controller.Quit, _) =>
        controller ! Controller.Quit
        stay()
      case Event(Controller.Finish, _) =>
        //Log.d(TAG, "LogonReactor received a Controller.Finish message")
        Ui( owner.getActivity.finish() ).run
        stop()
    }
  }
}
