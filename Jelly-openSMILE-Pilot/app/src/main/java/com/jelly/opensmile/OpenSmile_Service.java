package com.jelly.opensmile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.audeering.opensmile.androidtemplate.SmileJNI;
import com.jelly.constant.Constants;
import com.jelly.tarsos.Tarsos_VAD;
import com.opencsv.CSVReader;


import org.json.JSONException;
import org.json.JSONObject;
import org.radarcns.android.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by tiantianfeng on 10/18/17.
 */

public class OpenSmile_Service extends Service {

    private Handler mHandler = new Handler();
    private int i = 0;

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 4;

    private OpenSmilePlugins openSmilePlugins;
    private Config config;

    private String DEBUG = "TILEs";

    private OpenSmilePlugins smilePlugin;
    public boolean isActive = false;
    private String conf;

    private final int ENABLE_LIVE_OPEN_SMILE    = 1;
    private final int ENABLE_FULL_SMILE         = 2;
    private final int ENABLE_FILE_SAVING        = 3;

    private final boolean DEFINE_EXECUTOR       = false;

    private String dataPath, lldDataPath, rawAudioPath;
    private boolean isAllPermissionAllowed = true;

    private String jellyTokenID;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //if(startId < 6) {

            Log.d(DEBUG, "onStart: OpenSmile_Services");

            writeSharedPreference(Constants.OPENSMILERUN, Constants.OPENSMILERUNING);
            writeSharedPreference(Constants.OPENSMILE_CURRENT_ON, Constants.VAD_ON);

            // Make sure that if something wrong operating openSMILE, it will resume to opensmileVAD service
            mHandler.postDelayed(mTickExecutor, (Constants.OPENSMILE_DURATION + 10) * 1000);

            setupAssets();
            dataPath = getApplicationContext().getExternalFilesDir("") + "/audio_" + new Date().getTime() + ".csv";

            jellyTokenID = retrieveSharedPreference(Constants.QR_CODE_ID).substring(0, 4);

            MediaRecorder mRecorder = new MediaRecorder();
            mRecorder.reset();

            final int debug = ENABLE_FILE_SAVING;
            switch (debug) {
                case ENABLE_LIVE_OPEN_SMILE:

                    Log.d(DEBUG, "Live_openSMILE");

                    conf = getApplication().getCacheDir() + "/" + config.mainConf;

                    if(!DEFINE_EXECUTOR) {
                        try {
                            smilePlugin = new OpenSmilePlugins("test");

                        } catch (IOException e) {

                        }
                        isActive = true;
                        runOpenSMILEToFile(10);
                    }

                    break;

                case ENABLE_FILE_SAVING:

                    SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    String myDate = format.format(new Date());
                    /*
                    String wavPath = "TILEs/" + myDate + "/Wav";
                    File wavFile = new File(Environment.getExternalStorageDirectory(), wavPath);
                    if (!wavFile.exists()) {
                        if (!wavFile.mkdirs()) {
                            Log.e(DEBUG, "Problem creating folder");
                        }
                    }*/

                    String csvPath = "TILEs/" + myDate + "/csv";
                    File csvFile = new File(Environment.getExternalStorageDirectory(), csvPath);
                    if (!csvFile.exists()) {
                        if (!csvFile.mkdirs()) {
                            Log.e(DEBUG, "Problem creating folder");
                        }
                    }

                    String timestamp = Long.toString(new Date().getTime());

                    dataPath = Environment.getExternalStorageDirectory() + "/TILEs/"
                            + myDate + "/csv/audio_"
                            + jellyTokenID + "_"
                            + timestamp + ".csv";

                    rawAudioPath = Environment.getExternalStorageDirectory() + "/TILEs/"
                            + myDate + "/Wav/audio_"
                            + jellyTokenID + "_"
                            + timestamp + ".wav";

                    lldDataPath = Environment.getExternalStorageDirectory() + "/TILEs/" + myDate + "/csv/audio_" + jellyTokenID + "_"
                            + timestamp + "_lld" + ".csv";

                    Log.d(DEBUG, dataPath);
                    if(Constants.SAVE_WAV) {
                        conf = getApplication().getCacheDir() + "/" + config.saveDataConf;
                    } else {
                        conf = getApplication().getCacheDir() + "/" + config.saveDataConf_no_Wav;
                    }

                    if(retrieveSharedPreference(Constants.VAD_ON_OFF).equals(Constants.VAD_ON)) {
                        runOpenSMILEToFile(Constants.OPENSMILE_DURATION);
                    } else {
                        stopSelf();
                    }

                    break;

                default:
                    break;

            }
        //} else {
        //    stopSelfResult(startId);
        //    Log.d(Constants.DEBUG, "OpenSmile_Service->onStartCommand->stopSelfResult");
        //}



        return START_STICKY;
    }

    private String retrieveSharedPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(key, "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("TILEs", "onStop: OpenSmile_Services");

        /*
        *   Remove the mTickExecutor only when all permission is allowed
        * */
        if(isAllPermissionAllowed)
            mHandler.removeCallbacks(mTickExecutor);

        writeSharedPreference(Constants.VAD_STATUS, Constants.VAD_ALIVE);
        writeSharedPreference(Constants.VAD_GAP, Constants.VAD_GAP_FAST);
        writeSharedPreference(Constants.OPENSMILERUN, Constants.OPENSMILENOTRUNNING);
        writeSharedPreference(Constants.OPENSMILE_CURRENT_ON, Constants.VAD_OFF);

        if (Constants.RUN_OPENSMILEVAD){
            startOpenSmile_VAD();
        } else {
            startTarsosVAD();
        }
    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            File testFile = new File(dataPath);
            if(testFile.exists()) {
                Log.d(DEBUG, "test File exist!");
            }
            stopSelf();

        }
    };

    class SmileThread implements Runnable {

        @Override
        public void run() {
            SmileJNI.SMILExtractOutputJNI(conf, 1, dataPath);
        }
    }

    private void writeSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    class SmileWavAndFeatureThread implements Runnable {

        @Override
        public void run() {

            if(Constants.SAVE_WAV) {
                SmileJNI.SMILExtractWavAndFeatureJNI(conf, 1, dataPath, lldDataPath, rawAudioPath);
            } else {
                SmileJNI.SMILExtractOutputJNI(conf, 1, dataPath);
            }

        }
    }


    public void runOpenSMILEToFile(long second) {

        SmileJNI.prepareOpenSMILE(getApplicationContext());
        final SmileWavAndFeatureThread obj = new SmileWavAndFeatureThread();

        final Thread newThread = new Thread(obj);
        newThread.start();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                stopOpenSMILE();
                mHandler.removeCallbacks(mTickExecutor);
                Log.d("TILEs", "Stop recording in opensmile");

                stopSelf();
            }
        }, second * 1000);
    }


    public void stopOpenSMILE() {
        SmileJNI.SMILEndJNI();
    }

    void setupAssets() {
        ArrayList ans = new ArrayList<OpenSmilePlugins>();
        ans.add(openSmilePlugins);
        config = new Config(ans);
        String[] assets = config.assets;
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        String confcach = cw.getCacheDir() + "/";
        AssetManager assetManager = getAssets();
        for (String filename : assets) {
            try {
                InputStream in = assetManager.open(filename);
                String out = confcach + filename;
                File outFile = new File(out);
                FileOutputStream outst = new FileOutputStream(outFile);
                copyFile(in, outst);
                in.close();
                outst.flush();
                outst.close();
            } catch (IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private void startOpenSmile_VAD() {
        Intent openSmile_VAD_Service = new Intent(getApplicationContext(), OpenSmile_VAD.class);
        startService(openSmile_VAD_Service);
    }

    private void startTarsosVAD() {
        Intent Tarsos_Service = new Intent(getApplicationContext(), Tarsos_VAD.class);
        startService(Tarsos_Service);
    }

}
