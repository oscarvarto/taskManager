package com.optrak.experiment

import com.optrak.experiment.model._
import android.content.Context

/**
 * Created by oscarvarto on 2014/06/11.
 */
object workday {
  var mTasks = Vector.tabulate(4){ i =>
    val task = Task(i)
    task.title = "Task # " + i
    task.description = "Description # " + i
    task
  }

  var mAppContext: Option[Context] = None

  def apply(appContext: Context): this.type = {
    mAppContext = Some(appContext.getApplicationContext)
    this
  }

  def getTask(id: Int): Option[Task] = mTasks.find{ task => task.mId == id }

  def getTasks() = mTasks
}
