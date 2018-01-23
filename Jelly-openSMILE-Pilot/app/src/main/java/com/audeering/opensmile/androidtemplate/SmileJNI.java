/*
 Copyright (c) 2015 audEERING UG. All rights reserved.

 Date: 17.08.2015
 Author(s): Florian Eyben
 E-mail:  fe@audeering.com

 This is the interface between the Android app and the openSMILE binary.
 openSMILE is called via SMILExtractJNI()
 Messages from openSMILE are received by implementing the SmileJNI.Listener interface.
*/

package com.audeering.opensmile.androidtemplate;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.util.Log;

import com.jelly.opensmile.OpenSmile_VAD;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.compress.utils.IOUtils;

public class SmileJNI {

    public static final String[] assets = {
            "liveinput_android.conf",
            "liveinput_android_new.conf",
            "liveinput_android_radar.conf",
            "BufferModeRb.conf.inc",
            "BufferModeLive.conf.inc",
            "messages.conf.inc",
            "features.conf.inc",
            "jelly_arousal_valence.lld.conf.inc",
            "jelly_arousal_valence.conf",
            "e_jelly_arousal_valence.lld.conf.inc",
            "BufferModeRbLag.conf.inc",
            "BufferMode.conf.inc",
            "jelly_arousal_valence.func.conf.inc",
            "FrameModeFunctionals.conf.inc",
            "FrameModeFunctionalsLive.conf.inc",
            "e_jelly_arousal_valence.func.conf.inc",
            "eGeMAPSv01a_core.func.conf.inc",
            "GeMAPSv01a_core.lld.conf.inc",
            "GeMAPSv01a_core.func.conf.inc",
            "eGeMAPSv01a_core.lld.conf.inc",
            "emobase_live4.conf",
            "jelly_vad_opensource.conf",
            "message_vad_pitch.conf.inc",
            "jelly_vad_pitch.conf",
            "jelly_vad_NA.conf",
            "emobase_live4_no_wav.conf"
    };

    /**
     * load the JNI interface
     */
    static {
        System.loadLibrary("smile_jni");
    }


    /**
     * method to execute openSMILE binary from the Android app activity, see smile_jni.cpp.
     * @param configfile
     * @param externalStoragePath
     * @param updateProfile
     * @return
     */

    //public static native String SMILExtractJNI(String configFile, int updateProfile);
    public static native String SMILEVADJNI(String configFile, int updateProfile);
    public static native String SMILExtractJNI(String configFile, int updateProfile, String saveFolder);
    public static native String SMILExtractOutputJNI(String configFile, int updateProfile, String saveFolder);
    public static native String SMILExtractWavAndFeatureJNI(String configFile, int updateProfile, String saveFolder, String lldsaveFolder, String saveWav);
    public static native String SMILEndJNI();


    /**
     * process the messages from openSMILE (redirect to app activity etc.)
     */
    public interface Listener {
        void onSmileMessageReceived(String text);
    }

    public interface ThreadListener {
        void onFinishedRecording();
    }

    public static void  prepareOpenSMILE(Context c){
        setupAssets(c);
    }

    String conf;
    String recordingPath, wavPath;
    ThreadListener threadListenerlistener = null;
    Listener listener = null;
    private static boolean isRecording = false;
    public static boolean allowRecording = true;
    public static String lastRecording = "";


    private static Listener listener_;
    private static ThreadListener threadListener_;

    public static void registerListener (Listener listener) {
        listener_ = listener;
    }

    public static void registerListener (ThreadListener threadListener) {
        threadListener_ = threadListener;
    }

    /**
     * this is the first method called by openSMILE binary. it redirects the call to the Android
     * app activity.
     * @param text JSON encoded string
     */
    static void receiveText(String text) {
        if (listener_ != null)
            listener_.onSmileMessageReceived(text);
    }

    static void onFinishedRecording() {
        if (threadListener_ != null)
            threadListener_.onFinishedRecording();

    }

    public void runOpenSMILE(String conf, String recordingPath, long second) {
        this.conf = conf;
        this.recordingPath = recordingPath;
        this.wavPath = wavPath;

        final SmileThread obj = new SmileThread();
        final Thread newThread = new Thread(obj);
        newThread.start();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {

                if(isRecording) {
                    stopOpenSMILE();
                    Log.d("TILEs", "Stop recording in opensmile");
                }

                isRecording = false;
            }
        }, second * 1000);
    }

    public void runOpenSMILE(String conf, String recordingPath, String wavPath, long second) {
        this.conf = conf;
        this.recordingPath = recordingPath;

        final SmileWavAndFeatureThread obj = new SmileWavAndFeatureThread();
        final Thread newThread = new Thread(obj);
        newThread.start();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {

                if(isRecording) {
                    stopOpenSMILE();
                    Log.d("TILEs", "Stop recording in opensmile");
                }

                isRecording = false;
            }
        }, second * 1000);
    }

    public void runOpenSMILE() {

        final EnergyThread obj = new EnergyThread();
        final Thread newThread = new Thread(obj);
        newThread.start();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                stopOpenSMILE();
            }
        }, 10000);
    }

    class SmileVadThread implements Runnable {

        @Override
        public void run() {

            SmileJNI.SMILEVADJNI(conf, 1);
            threadListenerlistener.onFinishedRecording();

        }
    }

    public void runVAD(String conf, int seconds) {
        this.conf = conf;
        final SmileVadThread obj = new SmileVadThread();
        final Thread newThread = new Thread(obj);
        newThread.start();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                stopOpenSMILE();
            }
        }, seconds * 1000);
    }

    public static boolean getIsRecording(){
        return isRecording;
    }

    public void addListener(ThreadListener listener) {
        this.threadListenerlistener = listener;
    }

    public void addListener(Listener listener) {
        this.listener = listener;
    }

    public void stopOpenSMILE() {
        SmileJNI.SMILEndJNI();
    }

    class SmileThread implements Runnable {
        private volatile boolean paused = false;
        private final Object signal = new Object();

        @Override
        public void run() {
            //String recordingPath = getExternalFilesDir("") + "/test50.bin";
            File output = new File(recordingPath);
            lastRecording = recordingPath;
            if(output.exists()){
                output.delete();
            }
            if(allowRecording) {
                Log.d("TILEs", "start recording in opensmile");
                isRecording = true;
                try {
                    SmileJNI.SMILExtractJNI(conf, 1, recordingPath);
                }
                catch (Exception e){
                    lastRecording = "Error in openSMILE!";
                }

                threadListenerlistener.onFinishedRecording();

            }
        }
    }

    class SmileWavAndFeatureThread implements Runnable {
        private volatile boolean paused = false;
        private final Object signal = new Object();

        @Override
        public void run() {
            File output = new File(recordingPath);
            lastRecording = recordingPath;
            if(output.exists()){
                output.delete();
            }

            if(allowRecording) {
                Log.d("TILEs", "start recording in opensmile");
                isRecording = true;
                try {
                    SmileJNI.SMILExtractWavAndFeatureJNI(conf, 1, recordingPath, recordingPath, wavPath);
                }
                catch (Exception e){
                    lastRecording = "Error in openSMILE!";
                }

                threadListenerlistener.onFinishedRecording();

            }
        }
    }

    class EnergyThread implements Runnable {
        @Override
        public void run() {

            //SmileJNI.SMILExtractJNI(conf, 1);

        }
    }

    static void setupAssets (Context c){
        //SHOULD BE MOVED TO: /data/user/0/org.radarcns.opensmile/cache/
        ContextWrapper cw = new ContextWrapper(c);
        String confcach = cw.getCacheDir() + "/" ;//+ conf.mainConf;

        AssetManager assetManager = c.getAssets();

        for(String filename : assets) {
            String out = confcach + filename;
            File outFile = new File(out);

            try (InputStream in = assetManager.open(filename);
                 FileOutputStream outst = new FileOutputStream(outFile)) {
                IOUtils.copy(in, outst);
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    }
}
