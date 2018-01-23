package com.audeering.opensmile.androidtemplate

import android.app.Activity
import android.view.View
import android.util.Log
import com.audeering.opensmile.androidtemplate.plugins.{Config, Energy}

import scala.collection.mutable.ListBuffer

/*
 Copyright (c) 2016 audEERING UG. All rights reserved.

 Date: 08.08.2016
 Author(s): Gerhard Hagerer
 E-mail:  gh@audeering.com

  Here classes for receiving and showing the output values from openSMILE can be defined
  as plugins
*/


object SmilePlugin extends SmileJNI.Listener {

  val fragments = ListBuffer[Int]()
  val plugins = Config.plugins

  SmileJNI.registerListener(this)

  val TAG = "TILEs"

  // START shortcut methods
  def log(str: String) = Log.i(TAG, str)

  /**
    * this gets called when openSMILE sends a message to the app
    *
    * @param text JSON encoded string
    */
  override def onSmileMessageReceived(text: String) {
    // at first parse JSON
    val msg = new SmileMessage(text)
    val msgtype = msg("msgtype")
    val msgname = msg("msgname")

    // now see which SmilePlugins are there for that kind of openSMILE message (filtering)
    // and iterate over all SmilePlugins and execute their values
    //plugins.filter( _.filter(msg) ).foreach( _.updateValues(msg) )
    log(text)
  }

}

trait SmilePlugin {
  def updateValues(values: SmileMessage)
  def filter(jsn: SmileMessage): Boolean = true
  def updateData()
}