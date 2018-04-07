package com.jelly.tarsos;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jelly.application.Jelly_Application;
import com.jelly.constant.Constants;
import com.jelly.main.Main;
import com.jelly.opensmile.OpenSmile_Service;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor;

/**
 * Created by tiantianfeng on 11/4/17.
 */

public class Tarsos_VAD extends Service implements AudioProcessor, Thread.UncaughtExceptionHandler{

    private MediaRecorder mRecorder;

    private Handler mHandler = new Handler();
    private Handler ambientHandler = new Handler();
    private int i = 0;
    private File mOutputFile;
    private boolean isRecording = false;

    /*
    *   Pitch Detection
    * */
    static final boolean PITCH_DETECTOR_DEBUG_ENABLE = true;

    private double pitch;
    private PitchDetector pitchDetector;

    private final boolean ENABLE_ADAPTIVE       = false;

    private AudioDispatcher dispatcher;
    private double threshold = SilenceDetector.DEFAULT_SILENCE_THRESHOLD + 3;
    private SilenceDetector silenceDetector;

    private Handler vadHandler = new Handler();
    private Handler waitHandler = new Handler();


    private Thread pitchThread, soundThread;

    private ScheduledFuture<?> audioReadFuture;
    private ScheduledExecutorService executor;

    private int validPitchNumber = 0, validSoundNumber = 0;
    private int soundDb, pitchHz;

    private int VAD_GAP_TIME = 5;
    private int VAD_RUN_TIME = 10;
    private String DEBUG = "TILEs";

    private long numberOfVADRunedThisLifeCycle = 0;


    private boolean isVAD = false;
    private boolean isFoundVAD = false;
    private long startVADtime, lastVADtime, durationVAD = 0;
    private int numberOfVAD = 0;
    private int continuousVAD = 0;

    private PitchProcessor pitchProcessor;
    private AudioDispatcher mdispatcher;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //setAudioUpdateRate(10 + 10, 10);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (isFoundVAD) {
            startTarsosVADService();
            writeSharedPreference(Constants.VAD_CURRENT_ON, Constants.VAD_OFF);
            Log.d("TILEs", "onStop: tarsosVAD_Services");
        } else {
            startTarsosVADService();
            Log.d("TILEs", "onStop: tarsos_Services");
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        Intent intent = new Intent(getApplicationContext(), Main.class);
        intent.putExtra("crash", "crash");

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.
                getActivity(Jelly_Application.getInstance()
                        .getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager mgr = (AlarmManager) Jelly_Application.getInstance()
                .getBaseContext().getSystemService(Context.ALARM_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(retrieveSharedPreference(Constants.VAD_WRITE).contains(Constants.FALSE)) {
            writeSharedPreference(Constants.VAD_WRITE,  Constants.TRUE);
            writeSharedPreference(Constants.VAD_GAP,    Integer.toString(VAD_GAP_TIME));
            writeSharedPreference(Constants.VAD_RUN,    Integer.toString(VAD_RUN_TIME));
        } else {
            if(Constants.VAD_GAP_ADAPTIVE){
                VAD_GAP_TIME = Integer.parseInt(retrieveSharedPreference(Constants.VAD_GAP));
                Log.d(DEBUG, "onStart: TarsosVAD_Services: " + VAD_GAP_TIME);
            } else {
                VAD_GAP_TIME = 110;
                Log.d(DEBUG, "onStart: TarsosVAD_Services: " + VAD_GAP_TIME);
                writeSharedPreference(Constants.VAD_GAP, Integer.toString(VAD_GAP_TIME));
            }

        }

        Log.d(DEBUG, "onStart: TarsosVAD_Services");
        writeSharedPreference(Constants.VAD_CURRENT_ON, Constants.VAD_ON);

        isRecording = false;

        if(isAllPermissionGranted()) {
            executor = Executors.newSingleThreadScheduledExecutor();

            silenceDetector = new SilenceDetector(threshold, false);
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    mdispatcher = AudioDispatcherFactory.fromDefaultMicrophone(16000, 1024, 0);
                    mdispatcher.stop();
                    setAudioUpdateRate(VAD_GAP_TIME + VAD_RUN_TIME, VAD_RUN_TIME);
                    saveEnterDataToCSV();
                }
            }, 2500);
        } else {
            waitHandler.postDelayed(waitTickExecutor, (VAD_GAP_TIME + 30) * 1000);
        }

        return START_STICKY;
    }

    private boolean isAllPermissionGranted() {
        if(retrieveSharedPreference(Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean process(AudioEvent audioEvent) {

        if(silenceDetector.currentSPL() > threshold) {
            Log.d("TILEs", "Sound Detected: " + (int) silenceDetector.currentSPL() + " db SPL\n");
            validSoundNumber++;
        }

        return true;
    }

    @Override
    public void processingFinished() {

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

    private void setAudioUpdateRate(final int VAD_Time, final int VAD_Run_Time) {
        final int vad_time = VAD_Time;
        final int vad_run_time = VAD_Run_Time;
        synchronized (this) {
            if (audioReadFuture != null) {
                audioReadFuture.cancel(false);
            }
            audioReadFuture = executor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    Log.d(Constants.DEBUG, "setAudioUpdateRate");

                    if(retrieveSharedPreference(Constants.VAD_ON_OFF).equals(Constants.VAD_ON) &&
                            retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED)) {

                        if (numberOfVADRunedThisLifeCycle > 0) {
                            Log.d(Constants.DEBUG, "setAudioUpdateRate: 2nd");
                            executor.shutdown();
                            isFoundVAD = false;

                            if(Constants.VAD_GAP_ADAPTIVE) {
                                if (VAD_GAP_TIME < 20) {
                                    VAD_GAP_TIME = VAD_GAP_TIME + 5;
                                }

                                writeSharedPreference(Constants.VAD_GAP, Integer.toString(VAD_GAP_TIME));
                            }

                            mHandler.postDelayed(mTickExecutor, 100);
                        } else if (retrieveSharedPreference(Constants.OPENSMILERUN).contains(Constants.OPENSMILENOTRUNNING)) {
                            Log.d(Constants.DEBUG, "setAudioUpdateRate: 1st");
                            numberOfVADRunedThisLifeCycle++;
                            if (numberOfVADRunedThisLifeCycle % 5 == 0) {
                                saveDataToCSV();
                            }

                            if(!isRecording && mdispatcher.isStopped()) {

                                Log.d("TILEs", "Start " + vad_run_time  + " seconds VAD ");

                                writeSharedPreference(Constants.VAD_STATUS, Constants.VAD_ALIVE);

                                try {
                                    mdispatcher = AudioDispatcherFactory.fromDefaultMicrophone(16000, Constants.tarsos_window, Constants.tarsos_window_shift);
                                } catch (Exception e) {
                                }

                                isRecording = true;

                                PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {

                                    @Override
                                    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent e){
                                        if(pitchDetectionResult.isPitched()){
                                            Log.d("TILEs", "Pitch Detected: " + pitchDetectionResult.getPitch());
                                            pitch = pitchDetectionResult.getPitch();
                                            if(pitch < Constants.VALID_PITCH_HIGH && pitch > Constants.VALID_PITCH_LOW)
                                                validPitchNumber++;
                                            if(validPitchNumber > Constants.VALID_PITCH && validSoundNumber > Constants.VALID_SOUND) {

                                                vadHandler.removeCallbacks(vadRunExecutor);
                                                writeSharedPreference(Constants.OPENSMILERUN, Constants.OPENSMILERUNING);
                                                writeSharedPreference(Constants.VAD_TRIGGERED, Constants.TRUE);
                                                mdispatcher.stop();

                                                soundThread.interrupt();
                                                pitchThread.interrupt();

                                                mdispatcher.removeAudioProcessor(pitchProcessor);
                                                mdispatcher.removeAudioProcessor(silenceDetector);
                                                mdispatcher.removeAudioProcessor(Tarsos_VAD.this);

                                                isFoundVAD = true;
                                                mHandler.postDelayed(mTickExecutor, 1000);


                                            }
                                        }
                                    }
                                };

                                pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 16000, Constants.tarsos_window, pitchDetectionHandler);


                                try {
                                    mdispatcher.addAudioProcessor(silenceDetector);
                                    mdispatcher.addAudioProcessor(Tarsos_VAD.this);
                                    soundThread = new Thread(mdispatcher, "Sound Thread");
                                    soundThread.start();

                                    mdispatcher.addAudioProcessor(pitchProcessor);

                                    pitchThread = new Thread(mdispatcher, "Pitch Thread");
                                    pitchThread.start();
                                } catch (Exception e) {
                                    saveErrorToCSV(e.toString());
                                }

                                vadHandler.postDelayed(vadRunExecutor, VAD_RUN_TIME * 1000);

                            } else {
                                executor.shutdown();
                                saveErrorToCSV("Something Wrong");
                                isFoundVAD = false;
                                writeSharedPreference(Constants.VAD_GAP, Integer.toString(VAD_GAP_TIME));
                                Log.d(Constants.DEBUG, "Something Wrong");
                                //stopSelf();
                            }

                        } else {
                            saveErrorToCSV("Opensmile Shouldn't Run here!--------Tarsos VAD");
                        }

                    } else {
                        Log.d(DEBUG, "VAD DISABLED");
                        writeSharedPreference(Constants.VAD_STATUS, Constants.VAD_ALIVE);
                    }

                }
            }, 0, vad_time, TimeUnit.SECONDS);
        }
    }

    private void startOpenSmileService() {
        Intent openSMILE_Intent = new Intent(this, OpenSmile_Service.class);
        startService(openSMILE_Intent);
    }

    private void startTarsosVADService() {
        Intent tarsosVAD_Intent = new Intent(this, Tarsos_VAD.class);
        getApplicationContext().startService(tarsosVAD_Intent);

    }

    private Runnable waitTickExecutor = new Runnable() {
        @Override
        public void run() {
            waitHandler.removeCallbacks(waitTickExecutor);
            stopSelf();
        }
    };

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            mHandler.removeCallbacks(mTickExecutor);
            if (isFoundVAD) {
                startOpenSmileService();
            } else {
                Log.d(DEBUG, "mTickExecutor: stopSelf");
                Log.d(DEBUG, "mTickExecutor: " + Integer.parseInt(retrieveSharedPreference(Constants.VAD_INVALID_TIME)) );
                stopSelf();
            }

        }
    };

    private Runnable mAmbientExecutor = new Runnable() {
        @Override
        public void run() {
            ambientHandler.removeCallbacks(mAmbientExecutor);
            Log.d(DEBUG, "mAmbientExecutor: start ambient");
            Log.d(DEBUG, "mAmbientExecutor: " + Integer.parseInt(retrieveSharedPreference(Constants.VAD_INVALID_TIME)) );
            startOpenSmileService();
            writeSharedPreference(Constants.VAD_INVALID_TIME, "0");
        }
    };

    private Runnable vadRunExecutor = new Runnable() {
        @Override
        public void run() {

            vadHandler.removeCallbacks(vadRunExecutor);

            try {

                mdispatcher.stop();
                soundThread.interrupt();
                pitchThread.interrupt();

                mdispatcher.removeAudioProcessor(pitchProcessor);
                mdispatcher.removeAudioProcessor(silenceDetector);
                mdispatcher.removeAudioProcessor(Tarsos_VAD.this);

            } catch (Exception e) {
                saveErrorToCSV(e.toString() + "Tarsos VAD vadRun_exc");
            }

            isRecording = false;

            Log.d("TILEs", "Valid Sound = " + validSoundNumber + "--Valid Pitch = " + validPitchNumber);


            if(validPitchNumber > -1 && validSoundNumber > -1) {
                isFoundVAD = true;
                writeSharedPreference(Constants.VAD_TRIGGERED, Constants.TRUE);
                mHandler.postDelayed(mTickExecutor, 1000);
                writeSharedPreference(Constants.OPENSMILERUN, Constants.OPENSMILERUNING);
            } else if (validPitchNumber > -1) {
                isFoundVAD = true;
                mHandler.postDelayed(mTickExecutor, 1000);
                writeSharedPreference(Constants.VAD_TRIGGERED, Constants.TRUE);
                writeSharedPreference(Constants.OPENSMILERUN, Constants.OPENSMILERUNING);

            } else {
                isFoundVAD = false;
                writeSharedPreference(Constants.VAD_TRIGGERED, Constants.FALSE);
                int vad_invalid_time = Integer.parseInt(retrieveSharedPreference(Constants.VAD_INVALID_TIME));

                if (vad_invalid_time >= 6) {
                    ambientHandler.postDelayed(mAmbientExecutor, 1000);
                } else {
                    vad_invalid_time += 1;
                    writeSharedPreference(Constants.VAD_INVALID_TIME, Integer.toString(vad_invalid_time));
                }
            }

            validPitchNumber = 0;
            validSoundNumber = 0;

            writeSharedPreference(Constants.VAD_CURRENT_ON, Constants.VAD_IDLE);


            Log.d(DEBUG, "NUMBER OF VAD RUNNED = " + numberOfVADRunedThisLifeCycle + "---" + VAD_GAP_TIME);
        }
    };

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

    private void saveEnterDataToCSV() {

        String datapath = "TILEs";
        File file = new File(Environment.getExternalStorageDirectory(), datapath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(DEBUG, "Problem creating folder");
            }
        }

        String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/Enter_Log.csv";

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

    private void saveErrorToCSV(String error) {

        String datapath = "TILEs";
        File file = new File(Environment.getExternalStorageDirectory(), datapath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(DEBUG, "Problem creating folder");
            }
        }

        String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/Error_Log.csv";

        Calendar calendar = Calendar.getInstance();
        String vadTimestamp = Long.toString(new Date().getTime());


        try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true))) {

            String[] data = new String[2];

            data[0] = vadTimestamp;
            data[1] = error;

            writer.writeNext(data);
            writer.close();

        } catch (IOException e) {

        }

    }

}
