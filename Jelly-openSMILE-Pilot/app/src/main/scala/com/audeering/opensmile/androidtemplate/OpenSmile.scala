package com.audeering.opensmile.androidtemplate

import java.io._

import android.Manifest.permission._
import android.app.ProgressDialog
import android.content.pm.PackageManager._
import android.os.{Bundle, Handler}
import android.util.Log
import android.os.Environment
import android.app.Service
import android.support.v4.app._
import android.support.v4.content.ContextCompat

import android.os.Binder;
import android.os.IBinder
import android.content.Intent

import com.audeering.opensmile.androidtemplate.plugins.Config


class OpenSmile extends Service {

    val TAG = "TILEs"

    val assets = Config.assets
    def conf = getCacheDir + "/" + Config.mainConf

    val PERMISSION_REQUEST = 0
    val permissions = Array[String](RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)

    override def onCreate() {
    }

    override def onStart(intent: Intent, startid: Int)
    {
        log("Start OpenSmile")
        SmilePlugin
        assets.foreach(cacheAsset(_))

        if (!SmileThread.isActive) {
            SmileThread.start
            TimerThread.start
        } else {
            stopSelf
        }
    }

    override def onDestroy
    {
        if (SmileThread.isActive) {
          SmileThread.stop
        }
    }

    private final val localBinder = new LocalBinder()
    class LocalBinder extends Binder {
      def getService: OpenSmile = OpenSmile.this
    }

    override def onBind(intent: Intent): IBinder = localBinder


    object SmileThread {
        var isActive = false
        def start {
          (() => {
            isActive = true
            //SmileJNI.SMILExtractJNI(conf, 1)
          }).run
        }
        def stop {
          isActive = false
          SmileJNI.SMILEndJNI
        }
    }

    object TimerThread {
        var isActive = false
        def start {
          (() => {
            if (SmileThread.isActive) {
              SmileThread.stop
            }
            stopSelf
          }).runSenseDelayed(10000)
        }
        def stop {
            if (SmileThread.isActive) {
              SmileThread.stop
            }
            stopSelf
        }
    }

    /**
        * copies a file to a given destination
        *
        * @param filename the file to be copied
        * @param dst destination directory (default: cacheDir)
        */
    def cacheAsset(filename: String, dst: String = getCacheDir.toString) {
        val is = getAssets.open(filename)
        val outfile = new File(dst + "/" + filename)
        outfile.getParentFile.mkdirs()
        val os = new FileOutputStream(outfile)
        val buffer = new Array[Byte](50000)
        Stream.continually(is.read(buffer))
          .takeWhile(_ != -1)
          .foreach(os.write(buffer, 0, _))
        is.close
        os.flush
        os.close
    }

    // START shortcut methods
    def log(str: String) = Log.i(TAG, str)

    implicit class Closure(func: () => Any) {
        def asRunnable = new Runnable { override def run() = func() }
        def asThread   = new Thread(asRunnable)
        def run        = asThread.start()
        def runSenseDelayed(delay: Int) = new Handler().postDelayed(func.asRunnable, delay)
    }


}





