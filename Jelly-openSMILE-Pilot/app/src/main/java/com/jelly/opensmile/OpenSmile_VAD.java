package com.jelly.opensmile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.audeering.opensmile.androidtemplate.SmileJNI;
import com.jelly.constant.Constants;
import com.opencsv.CSVWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by tiantianfeng on 10/27/17.
 */

public class OpenSmile_VAD extends Service {

    private Handler mHandler = new Handler();
    private int i = 0;

    private OpenSmilePlugins openSmilePlugins;
    private Config config;

    private String DEBUG = "TILEs";

    private OpenSmilePlugins smilePlugin;
    public boolean isActive = false;
    private String conf;

    private final int ENABLE_LIVE_OPEN_SMILE    = 1;
    private final int ENABLE_FULL_SMILE         = 2;
    private final int ENABLE_FILE_SAVING        = 3;

    private final boolean ENABLE_OPENSMILE      = true;
    private final boolean DEFINE_EXECUTOR       = true;
    private final boolean ENABLE_DEBUG_POWER    = false;
    private final boolean ENABLE_ADAPTIVE       = true;

    private final String VAD_GAP                = "VAD_GAP";
    private final String VAD_RUN                = "VAD_RUN";

    private String dataPath;

    private ScheduledFuture<?> audioReadFuture;
    private ScheduledExecutorService executor;

    private boolean isAllPermissionAllowed = true;

    private boolean isVAD = false;
    private boolean isFoundVAD = false;
    private long startVADtime, lastVADtime, durationVAD = 0;
    private int numberOfVAD = 0;
    private int continuousVAD = 0;

    private int VAD_GAP_TIME = 5;
    private int VAD_RUN_TIME = 10;

    private boolean RESTART = true;

    private long numberOfVADRunedThisLifeCycle = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private String retrieveSharedPreference(String key) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(key, "");

    }

    private void writeSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        if(!isFoundVAD) {
            Intent openSMILE_Intent = new Intent(this, OpenSmile_VAD.class);
            //startService(openSMILE_Intent);
        }

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //onTaskRemoved(intent);

        if(retrieveSharedPreference(Constants.VAD_WRITE).contains(Constants.FALSE)) {
            writeSharedPreference(Constants.VAD_WRITE,  Constants.TRUE);
            writeSharedPreference(Constants.VAD_GAP,    Integer.toString(VAD_GAP_TIME));
            writeSharedPreference(Constants.VAD_RUN,    Integer.toString(VAD_RUN_TIME));
        } else {
            VAD_GAP_TIME = Integer.parseInt(retrieveSharedPreference(Constants.VAD_GAP));
            Log.d(DEBUG, "onStart: OpenSmileVAD_Services: " + VAD_GAP_TIME);
        }

        Log.d(DEBUG, "onStart: OpenSmileVAD_Services");

        executor = Executors.newSingleThreadScheduledExecutor();

        setupAssets();

        final int debug = ENABLE_LIVE_OPEN_SMILE;
        switch (debug) {
            case ENABLE_LIVE_OPEN_SMILE:

                Log.d(DEBUG, "Live_openSMILE_VAD");
                conf = getApplication().getCacheDir() + "/" + config.vadConf;
                setAudioUpdateRate(VAD_GAP_TIME + VAD_RUN_TIME, VAD_RUN_TIME);

                break;

            default:
                break;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isFoundVAD) {
            startOpenSmileService();
            Log.d("TILEs", "onStop: OpenSmileVAD_Services");
        } else {
            startOpenSmileVADService();
            Log.d("TILEs", "onStop: OpenSmile_Services");
        }

        mHandler.removeCallbacks(mTickExecutor);

    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(mTickExecutor);
            stopSelf();
        }
    };


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
                if (filename.contains("vad")) {
                    InputStream in = assetManager.open(filename);
                    String out = confcach + filename;
                    File outFile = new File(out);
                    FileOutputStream outst = new FileOutputStream(outFile);
                    copyFile(in, outst);
                    in.close();
                    outst.flush();
                    outst.close();
                }

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

    private void setAudioUpdateRate(final int VAD_Time, final int VAD_Run_Time) {
        final int vad_time = VAD_Time;
        final int vad_run_time = VAD_Run_Time;
        SmileJNI.prepareOpenSMILE(getApplicationContext());

        synchronized (this) {
            if (audioReadFuture != null) {
                audioReadFuture.cancel(false);
            }
            audioReadFuture = executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {

                    Log.d("TILEs", "Start " + vad_run_time  + " seconds VAD ");

                    numberOfVADRunedThisLifeCycle++;
                    if (numberOfVADRunedThisLifeCycle % 5 == 0) {
                        saveDataToCSV();
                    }


                    isVAD = false;
                    isFoundVAD = false;
                    startVADtime = new Date().getTime();
                    numberOfVAD = 0;

                    final SmileJNI smileJNI = new SmileJNI();

                    SmileJNI.ThreadListener threadListener = new SmileJNI.ThreadListener() {
                        @Override
                        public void onFinishedRecording() {
                            Log.d("TILEs", "Finish " + vad_run_time  + " seconds VAD " + "VAD Gap: " + (vad_time - vad_run_time));

                            writeSharedPreference(Constants.VAD_STATUS, Constants.VAD_ALIVE);

                            if(isFoundVAD) {

                                durationVAD = lastVADtime - startVADtime;
                                isVAD = false;
                                //isFoundVAD = false;
                                numberOfVAD = 0;

                                Log.d("TILEs", "VAD Duration: " + durationVAD);

                                if (!ENABLE_DEBUG_POWER) {
                                    executor.shutdown();
                                    mHandler.postDelayed(mTickExecutor, 8500);
                                }

                                continuousVAD++;

                                if(continuousVAD > 3) {
                                }

                            } else {
                                if (ENABLE_ADAPTIVE) {
                                    if (VAD_GAP_TIME < 25) {
                                        if (numberOfVADRunedThisLifeCycle % 2 == 0) {
                                            executor.shutdown();
                                            VAD_GAP_TIME = VAD_GAP_TIME + 5;
                                            isFoundVAD = false;
                                            writeSharedPreference(Constants.VAD_GAP, Integer.toString(VAD_GAP_TIME));

                                            stopSelf();
                                        }
                                    }
                                }
                            }



                        }
                    };

                    SmileJNI.Listener listener = new SmileJNI.Listener() {
                        @Override
                        public void onSmileMessageReceived(String text) {

                            final String t = text;

                            try {
                                JSONObject jo = new JSONObject(t);

                                double data1 = jo.getJSONObject("floatData").getDouble("0");
                                double data2 = jo.getJSONObject("floatData").getDouble("1");
                                double data3 = jo.getJSONObject("floatData").getDouble("2");
                                double data4 = jo.getJSONObject("floatData").getDouble("3");
                                double data5 = jo.getJSONObject("floatData").getDouble("4");
                                double data6 = jo.getJSONObject("floatData").getDouble("5");

                                if(data1 == 1) {

                                    long timeNow = new Date().getTime();

                                    if (isVAD) {
                                        if (timeNow - lastVADtime < 1500) {
                                            numberOfVAD++;
                                            if (numberOfVAD > 55) {
                                                if (ENABLE_OPENSMILE) {
                                                    if (!ENABLE_DEBUG_POWER) {
                                                        SmileJNI.SMILEndJNI();
                                                    }
                                                    isVAD = false;
                                                    isFoundVAD = true;
                                                }
                                            }
                                        }
                                        lastVADtime = timeNow;
                                    } else {
                                        startVADtime = timeNow;
                                        lastVADtime = timeNow;
                                        isVAD = true;
                                    }
                                }

                                if(data1 == 1) {

                                    //Log.d("Tiles:", "VAD FOUND" + ": " + data1 + "---" + data2 + "---"
                                    //        + data3 + "---" + data4);
                                    Log.d("Tiles:", "VAD FOUND" + ": " + data1);
                                    //Log.d("Tiles:", "VAD FOUND" + ": " + jo.toString());
                                }

                                if(data2 != 0 || data3 != 0 || data4 != 0 || data5 != 0 || data6 != 0) {

                                    Log.d("Tiles:", "VAD FOUND" + ": " + data1 + "---" + data2 + "---"
                                        + data3 + "---" + data4 + "---" + data5 + "---" + data6);
                                }

                                //Log.d(DEBUG, jo.toString());


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    smileJNI.registerListener(threadListener);
                    smileJNI.addListener(threadListener);

                    smileJNI.registerListener(listener);
                    smileJNI.addListener(listener);

                    smileJNI.runVAD(conf, vad_run_time);
                }
            }, 0, vad_time, TimeUnit.SECONDS);
        }
    }

    private void quickVAD_Mode(int duration) {
        final int recordLength = duration;
        SmileJNI.prepareOpenSMILE(getApplicationContext());

        synchronized (this) {
            if (audioReadFuture != null) {
                audioReadFuture.cancel(false);
            }
            audioReadFuture = executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {

                    Log.d("TILEs", "Start 5 seconds VAD ");

                    isVAD = false;
                    isFoundVAD = false;
                    startVADtime = new Date().getTime();
                    numberOfVAD = 0;

                    final SmileJNI smileJNI = new SmileJNI();

                    SmileJNI.ThreadListener threadListener = new SmileJNI.ThreadListener() {
                        @Override
                        public void onFinishedRecording() {
                            Log.d("TILEs", "Finish 5 seconds VAD ");
                            if(isFoundVAD) {

                                durationVAD = lastVADtime - startVADtime;
                                isVAD = false;
                                isFoundVAD = false;
                                numberOfVAD = 0;

                                Log.d("TILEs", "VAD Duration: " + durationVAD);
                                executor.shutdown();
                                //mHandler.postDelayed(mTickExecutor, 4000);
                                continuousVAD++;

                                if(continuousVAD > 3) {


                                }

                            }
                            smileJNI.stopOpenSMILE();
                        }
                    };

                    SmileJNI.Listener listener = new SmileJNI.Listener() {
                        @Override
                        public void onSmileMessageReceived(String text) {
                            final String t = text;

                            try {
                                JSONObject jo = new JSONObject(t);

                                double data1 = jo.getJSONObject("floatData").getDouble("0");
                                double data2 = jo.getJSONObject("floatData").getDouble("1");

                                if(data1 == 1 && data2 < 300 && data2 > 60) {

                                    long timeNow = new Date().getTime();

                                    if (isVAD) {
                                        if (timeNow - lastVADtime < 1000) {
                                            numberOfVAD++;
                                            if (numberOfVAD > 50) {
                                                if (ENABLE_OPENSMILE) {
                                                    SmileJNI.SMILEndJNI();
                                                    smileJNI.stopOpenSMILE();
                                                    isVAD = false;
                                                    isFoundVAD = true;
                                                }
                                            }
                                        }
                                        lastVADtime = timeNow;
                                    } else {
                                        startVADtime = timeNow;
                                        lastVADtime = timeNow;
                                        isVAD = true;
                                    }


                                    Log.d("Tiles:", "VAD FOUND" + ": " + data1 + "---" + data2);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    };

                    smileJNI.registerListener(threadListener);
                    smileJNI.addListener(threadListener);

                    smileJNI.registerListener(listener);
                    smileJNI.addListener(listener);

                    smileJNI.runVAD(conf, 5);
                }
            }, 0, recordLength, TimeUnit.SECONDS);
        }
    }


    private void saveDataToCSV(boolean isFoundVAD) {

        String datapath = "TILEs";
        File file = new File(Environment.getExternalStorageDirectory(), datapath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(DEBUG, "Problem creating folder");
            }
        }

        String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/VAD_Log.csv";

        String timestamp;
        timestamp = Long.toString(new Date().getTime());


        try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true))) {

            String[] data = new String[2];

            data[0] = timestamp;
            if (isFoundVAD) {
                data[1] = Long.toString(durationVAD);
            } else {
                data[1] = Long.toString(0);
            }


            writer.writeNext(data);

            writer.close();

        } catch (IOException e) {

        }
    }

    private void startOpenSmileService() {
        Intent openSMILE_Intent = new Intent(this, OpenSmile_Service.class);
        startService(openSMILE_Intent);

    }

    private void startOpenSmileVADService() {
        Intent openSMILEVAD_Intent = new Intent(this, OpenSmile_VAD.class);
        startService(openSMILEVAD_Intent);

    }

    private void saveDataToCSV() {

        String datapath = "TILEs";
        File file = new File(Environment.getExternalStorageDirectory(), datapath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(DEBUG, "Problem creating folder");
            }
        }

        String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/VAD_Log.csv";

        Calendar calendar = Calendar.getInstance();
        String vadTimestamp = Long.toString(new Date().getTime());


        try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true))) {

            String[] data = new String[2];

            data[0] = vadTimestamp;
            data[1] = Long.toString(numberOfVADRunedThisLifeCycle);

            writer.writeNext(data);
            writer.close();

        } catch (IOException e) {

        }

    }

    public boolean clearCache() {
        try {
            File[] files = getBaseContext().getCacheDir().listFiles();

            for (File file : files) {
                // delete returns boolean we can use
                if (!file.delete()) {
                    return false;
                }
            }
            // if for completes all
            return true;

        } catch (Exception e) {}

        // try stops clearing cache
        return false;
    }
}
