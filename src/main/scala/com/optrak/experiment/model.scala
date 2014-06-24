package com.optrak.experiment

import java.util.Date

/**
 * Created by oscarvarto on 2014/06/11.
 */
object model {
  case class Task(mId: Int) {
    var title: String = "Task Title"
    var description: String = "Put a description here"
    var mDone: Boolean = false
    val mDate = new Date()
  }

  case class Initialiser(something: Int = 0, desc: String = "Hello")
}
