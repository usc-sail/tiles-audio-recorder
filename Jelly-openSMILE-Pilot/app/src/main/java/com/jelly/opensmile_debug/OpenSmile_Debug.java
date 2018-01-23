package com.jelly.opensmile_debug;

import android.app.Service;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import com.audeering.opensmile.androidtemplate.SmileJNI;
import com.jelly.opensmile.Config;
import com.jelly.opensmile.OpenSmilePlugins;
import com.jelly.opensmile.OpenSmile_VAD;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by tiantianfeng on 10/31/17.
 */

public class OpenSmile_Debug extends Service {

    private Handler mHandler = new Handler();

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

    private String dataPath, rawAudioPath, lldDataPath;
    private boolean isAllPermissionAllowed = true;

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

        Log.d(DEBUG, "onStart: OpenSmile_Services");

        // Make sure that if something wrong operating openSMILE, it will resume to opensmileVAD service
        mHandler.postDelayed(mTickExecutor, 90 * 1000);

        setupAssets();
        dataPath = getApplicationContext().getExternalFilesDir("") + "/audio_" + new Date().getTime() + ".csv";

        String prefix = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        prefix = prefix.substring(prefix.length() - 4);

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
                    runOpenSMILEToFile(60);
                }

                break;

            case ENABLE_FILE_SAVING:

                SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                String myDate = format.format(new Date());

                String wavPath = "TILEs/Debug/" + myDate + "/Wav";
                File wavFile = new File(Environment.getExternalStorageDirectory(), wavPath);
                if (!wavFile.exists()) {
                    if (!wavFile.mkdirs()) {
                        Log.e(DEBUG, "Problem creating folder");
                    }
                }

                String csvPath = "TILEs/Debug/" + myDate + "/csv";
                File csvFile = new File(Environment.getExternalStorageDirectory(), csvPath);
                if (!csvFile.exists()) {
                    if (!csvFile.mkdirs()) {
                        Log.e(DEBUG, "Problem creating folder");
                    }
                }

                String timestamp = Long.toString(new Date().getTime());

                dataPath = Environment.getExternalStorageDirectory() + "/TILEs/Debug/" + myDate + "/csv/audio_"
                        + timestamp + "_" + prefix + ".csv";

                rawAudioPath = Environment.getExternalStorageDirectory() + "/TILEs/Debug/" + myDate + "/Wav/audio_"
                        + timestamp + "_" + prefix + ".wav";

                lldDataPath = Environment.getExternalStorageDirectory() + "/TILEs/" + myDate + "/csv/audio_"
                        + timestamp + "_" + prefix + "_lld" + ".csv";

                Log.d(DEBUG, dataPath);
                conf = getApplication().getCacheDir() + "/" + config.debugConf;
                runOpenSMILEToFile(60);

                break;

            default:
                break;
        }

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


    class SmileWavAndFeatureThread implements Runnable {

        @Override
        public void run() {
            SmileJNI.SMILExtractWavAndFeatureJNI(conf, 1, dataPath, lldDataPath, rawAudioPath);
        }
    }


    public void runOpenSMILEToFile(long second) {

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
}
