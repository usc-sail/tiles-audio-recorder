package com.jelly.battery;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jelly.application.Jelly_Application;
import com.jelly.ble_service.BLE_Advertise_Service;
import com.jelly.ble_service.BLE_Scan_Service;
import com.jelly.constant.Constants;
import com.jelly.main.Main;
import com.jelly.opensmile.OpenSmile_VAD;
import com.jelly.tarsos.Tarsos_VAD;
import com.jelly.wifi.WifiScanService;
import com.opencsv.CSVWriter;

import org.radarcns.android.MainActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Created by tiantianfeng on 10/13/17.
 */

public class Battery_Service extends Service {

    private String DEBUG_FILE   = "TILEs_File";

    private final int CSV_DATA_ENTRY_SIZE = 6;

    private String batteryLevel;
    private String batteryTimestamp;
    private String batteryChargingStatus = "NA";
    private Handler mHandler = new Handler();

    private String vad_gap, vad_run;

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 5;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getBatteryLevel() {
        Intent intent  = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int    percent = (level*100)/scale;
        Log.d("TILEs", String.valueOf(percent) + "%");

        return String.valueOf(percent) + "%";
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TILEs", "onStart:Battery_Services");

        batteryLevel = getBatteryLevel();
        retrieveSharedPreference();

        /*
        *   Start BLE service
        * */
        if(retrieveSharedPreference(Constants.BLE_STATUS).contains(Constants.BLE_OFF)) {
            startBLEAdvService();
            startBLEScanService();
        } else {
            writeSharedPreference(Constants.BLE_STATUS, Constants.BLE_OFF);
        }

        /*
        *   Make sure VAD is not Dead
        * */
        if(retrieveSharedPreference(Constants.VAD_STATUS).contains(Constants.VAD_DEAD)) {
            Log.d(DEBUG_FILE, "VAD START");
            if(retrieveSharedPreference(Constants.VAD_STATUS).contains(Constants.VAD_INIT)) {
                mHandler.postDelayed(mTickExecutor, 10000);
                if (Constants.RUN_OPENSMILEVAD){
                    startOpenSmile_VAD();
                } else {
                    writeSharedPreference(Constants.OPENSMILERUN, Constants.OPENSMILENOTRUNNING);
                    startTarsosVAD();
                }
            } else {
                if (isAllPermissionGranted()) {
                    saveErrorToCSV("Force start!");
                }
                startMainActivity();
            }


        } else {
            writeSharedPreference(Constants.VAD_STATUS, Constants.VAD_DEAD);
            writeSharedPreference(Constants.OPENSMILERUN, Constants.OPENSMILENOTRUNNING);
            Log.d(DEBUG_FILE, "VAD ALREADY START");
            mHandler.postDelayed(mTickExecutor, 10000);
        }

        /*
        *   Make sure we have turned it back on
        * */
        if(retrieveSharedPreference(Constants.VAD_ON_OFF).contains(Constants.VAD_OFF)) {
            Log.d(Constants.DEBUG, "Battery_Service->onCreate->" + Integer.parseInt(retrieveSharedPreference(Constants.VAD_OFF_TIME)));
            if(Integer.parseInt(retrieveSharedPreference(Constants.VAD_OFF_TIME)) >= 5) {
                writeSharedPreference(Constants.VAD_OFF_TIME, "0");
                writeSharedPreference(Constants.VAD_ON_OFF, Constants.VAD_ON);
            } else {
                int timeOff = Integer.parseInt(retrieveSharedPreference(Constants.VAD_OFF_TIME)) + 1;
                writeSharedPreference(Constants.VAD_OFF_TIME, Integer.toString(timeOff));
            }
        }

        /*
        *   Start Wifi Service
        * */
        //startWifiService();

        WifiManager mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (mainWifi.isWifiEnabled() == true && retrieveSharedPreference(Constants.UPLOAD_STATUS).contains(Constants.UPLOAD_OFF)) {
            mainWifi.setWifiEnabled(false);
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startOpenSmile_VAD() {
        Intent openSmile_VAD_Service = new Intent(getApplicationContext(), OpenSmile_VAD.class);
        startService(openSmile_VAD_Service);
    }

    private void startTarsosVAD() {
        Intent Tarsos_Service = new Intent(getApplicationContext(), Tarsos_VAD.class);
        Tarsos_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(Tarsos_Service);
    }

    private void startMainActivity() {
        Intent Main_Activity = new Intent(getApplicationContext(), Main.class);

        Main_Activity.setAction(Intent.ACTION_MAIN);
        Main_Activity.addCategory(Intent.CATEGORY_LAUNCHER);
        Main_Activity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        getApplication().getApplicationContext().startActivity(Main_Activity);
        stopSelf();
    }

    /*
    *   Start BLE Service
    * */
    private void startBLEAdvService() {
        if(Constants.ENABLE_BLE_ADV_RUN) {
            Log.d(DEBUG_FILE, "BLE ADV START");
            Intent ble_adv_Service = new Intent(getApplicationContext(), BLE_Advertise_Service.class);
            ble_adv_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            getApplication().getApplicationContext().startService(ble_adv_Service);
        }
    }

    private void startBLEScanService() {
        if(Constants.ENABLE_BLE_SCAN_RUN) {
            Log.d(DEBUG_FILE, "BLE SCAN START");
            Intent ble_scan_Service = new Intent(getApplicationContext(), BLE_Scan_Service.class);
            ble_scan_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            getApplication().getApplicationContext().startService(ble_scan_Service);
        }
    }

    private void startWifiService() {
        Intent wifi_Service = new Intent(getApplicationContext(), WifiScanService.class);
        wifi_Service.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        getApplication().getApplicationContext().startService(wifi_Service);
    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            if(isAllPermissionGranted()) {
                saveDataToCSV();
            }

            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent battery_Intent = new Intent(getApplicationContext(), Battery_Service.class);
            pendingIntent = PendingIntent.getService(getApplicationContext(), 1, battery_Intent, PendingIntent.FLAG_ONE_SHOT);

            if(Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
                Log.d("Tiles", "Battery Set Alarm Service");
            }
            else if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            }

            stopSelf();


        }
    };

    private boolean isAllPermissionGranted() {
        if(retrieveSharedPreference(Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

    private void saveDataToCSV() {

        String datapath = "TILEs";
        File file = new File(Environment.getExternalStorageDirectory(), datapath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(DEBUG_FILE, "Problem creating folder");
            }
        }

        String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/Battery_Log.csv";

        Calendar calendar = Calendar.getInstance();
        batteryTimestamp = Long.toString(new Date().getTime());


        try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true))) {

            String[] data = new String[CSV_DATA_ENTRY_SIZE];

            data[0] = batteryTimestamp;
            data[1] = batteryLevel;
            data[2] = vad_gap;
            data[3] = vad_run;
            data[4] = batteryChargingStatus;

            if (Constants.RUN_OPENSMILEVAD){
                data[5] = "Energy";
            } else {
                data[5] = "Tarsos";
            }
            writer.writeNext(data);
            writer.close();

        } catch (IOException e) {
            //saveErrorToCSV(e.toString());
        }

    }

    private void retrieveSharedPreference() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getString(Constants.VAD_WRITE, "").contains("FALSE")) {
            this.vad_gap = "NA";
            this.vad_run = "NA";
        } else {
            this.vad_gap = preferences.getString(Constants.VAD_GAP, "");
            this.vad_run = preferences.getString(Constants.VAD_RUN, "");
        }

    }

    private void writeSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String retrieveSharedPreference(String key) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(key, "");

    }

    private void saveErrorToCSV(String error) {

        String datapath = "TILEs";
        File file = new File(Environment.getExternalStorageDirectory(), datapath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("Tiles", "Problem creating folder");
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
