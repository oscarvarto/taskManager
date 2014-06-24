package com.optrak

import java.util.concurrent.{TimeUnit, ThreadPoolExecutor, LinkedBlockingQueue}

import akka.actor.ActorSelection
import android.widget.Button
import macroid._
import macroid.FullDsl._
import macroid.util.Ui

import scala.concurrent.ExecutionContext

/**
 * Created by oscarvarto on 2014/06/11.
 */
package object experiment {
  type JList[A] = java.util.List[A]
  val TAG = "Debug"

  val (corePoolSize, maximumPoolSize, keepAliveTime) = (30, 30, 100)
  val workQueue = new LinkedBlockingQueue[Runnable]
  // Execution context for futures below
  implicit val exec = ExecutionContext.fromExecutor(
    new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue)
  )

  val UserInterfaceNotReady = "User interface must exist before calling updateUserInterface"

  trait PMsg { // For messages that go through FSMProcessor
    def destination: String
  }
  
  def bombTweak(AAName: String): Tweak[Button] = On.click { Ui(throw new Exception("Bomb in " + AAName)) }
}
