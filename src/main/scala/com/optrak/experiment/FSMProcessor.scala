package com.optrak.experiment

import akka.actor.{Actor, ActorPath, Props}
import akka.persistence._
import android.util.Log
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by oscarvarto on 2014/06/19.
 */
object FSMProcessor {
  val Name = "FSMProcessor"
  def props(): Props = Props(new FSMProcessor)
}

class FSMProcessor extends Processor {

  @scala.throws[Exception](classOf[Exception]) override
  def preStart(): Unit = {
    super.preStart()
    Log.d(TAG, "FSMProcessor#preStart() called, after calling self ! Recover")
  }

  val channelSettings = ChannelSettings(
    redeliverMax = 10,
    redeliverInterval = 0.1 seconds,
    redeliverFailureListener = Some(context.actorOf(MsgNotDeliveredListener.props(), "MsgNotDeliveredListener"))
  )  
  val fsmChannel = context.actorOf(Channel.props(channelSettings), "fsmChannel")

  def receive: Receive = {
    case p @ Persistent(pmsg: PMsg, _) =>
      val pth = ActorPath.fromString(s"akka://${ActorSystemManager.ActorSystemName}" + pmsg.destination)
      Log.d(TAG, s"$pmsg received in FSMProcessor")
      fsmChannel ! Deliver(p.withPayload(pmsg), pth)
    case msg =>
      Log.d(TAG, s"----> Non Persistent $msg received in FSMProcessor")
  }
}

object MsgNotDeliveredListener {
  def props(): Props = Props(new MsgNotDeliveredListener)
}
class MsgNotDeliveredListener extends Actor {
  def receive = {
    case RedeliverFailure(msg) =>
      Log.d(TAG, s"$msg failed to be delivered")
      // Vector(ConfirmablePersistentImpl(StartSecondAA,5,/user/FSMProcessor,false,5,List(),DeliveredByChannel(/user/FSMProcessor,/user/FSMProcessor/fsmChannel,5,1,Actor[akka://Experiment/user/FSMProcessor/fsmChannel#-600402820]),Actor[akka://Experiment/system/confirmation-batch-layer#-145929017],Actor[akka://Experiment/user/LogonActivityActor#-55938224]), ConfirmablePersistentImpl(StartSecondAA,8,/user/FSMProcessor,false,5,List(),DeliveredByChannel(/user/FSMProcessor,/user/FSMProcessor/fsmChannel,8,2,Actor[akka://Experiment/user/FSMProcessor/fsmChannel#-600402820]),Actor[akka://Experiment/system/confirmation-batch-layer#-145929017],Actor[akka://Experiment/user/LogonActivityActor#-771848658])) failed to be delivered
  }
}