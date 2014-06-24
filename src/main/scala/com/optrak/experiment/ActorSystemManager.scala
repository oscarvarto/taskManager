package com.optrak.experiment

import akka.actor.ActorSystem
import android.content.Context
import scalaz.syntax.std.option._

/**
 * Created by oscarvarto on 2014/06/12.
 */
object ActorSystemManager {
  System.setProperty("sun.arch.data.model", "32")
  System.setProperty("leveldb.mmap", "false")

  var mAppContext: Option[Context] = None

  val ActorSystemName = "Experiment"
  val actorSys = ActorSystem(ActorSystemName)
  val controller = actorSys.actorOf(Controller.props(), Controller.Name)
  val fsmProcessor = actorSys.actorOf(FSMProcessor.props(), FSMProcessor.Name)

  def apply(ctx: Context): this.type = {
    mAppContext = ctx.getApplicationContext.some
    this
  }
}
