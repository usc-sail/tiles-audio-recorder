package com.jelly.wifi;

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
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jelly.constant.Constants;

import java.util.List;

/**
 * Created by tiantianfeng on 12/22/17.
 */

public class WifiScanService extends Service {

    private StringBuilder sb = new StringBuilder();
    private List<ScanResult> wifiList;
    private WifiManager mainWifi;
    private WifiReceiver receiverWifi;
    private Handler mHandler = new Handler();

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60 * 5;

    private boolean isAtWorking = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        // Initiate wifi service manager
        mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check for wifi is disabled
        if (mainWifi.isWifiEnabled() == false)
        {
            Log.d(Constants.DEBUG, "WifiScanService->onStartCommand->enable wifi");

            mainWifi.setWifiEnabled(true);
        }

        // wifi scaned value broadcast receiver
        receiverWifi = new WifiReceiver();

        // Register broadcast receiver
        // Broacast receiver will automatically call when number of wifi connections changed
        //getApplicationContext().registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();

        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    wifiList = mainWifi.getScanResults();
                    Log.d(Constants.DEBUG, "WifiScanService->WifiReceiver->" + mainWifi.getScanResults().size());

                    for(int i = 0; i < wifiList.size(); i++){
                        Log.d(Constants.DEBUG, wifiList.get(i).toString());
                        if(wifiList.get(i).toString().contains("SLOW")) {
                            isAtWorking = true;
                        }
                    }
                }
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mHandler.postDelayed(mTickExecutor, 50000);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //getApplicationContext().unregisterReceiver(receiverWifi);
        //mainWifi.setWifiEnabled(false);

        if(isAtWorking) {
            writeSharedPreference(Constants.WORK_WIFI, Constants.WORK_WIFI_ON);
            writeSharedPreference(Constants.WORK_WIFI_OFF_TIME, "0");
        } else {

            int wifiOffTime = Integer.parseInt(retrieveSharedPreference(Constants.WORK_WIFI_OFF_TIME));
            if(wifiOffTime > 2) {
                writeSharedPreference(Constants.WORK_WIFI, Constants.WORK_WIFI_OFF);
                writeSharedPreference(Constants.WORK_WIFI_OFF_TIME, "0");
            } else {
                wifiOffTime = wifiOffTime + 1;
                writeSharedPreference(Constants.WORK_WIFI_OFF_TIME, Integer.toString(wifiOffTime));
            }

        }
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {


        }

    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {

            /*
            alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            Intent wifi_Intent = new Intent(getApplicationContext(), WifiScanService.class);
            pendingIntent = PendingIntent.getService(getApplicationContext(), 1, wifi_Intent, PendingIntent.FLAG_ONE_SHOT);

            if(Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
                Log.d("Tiles", "Battery Set Alarm Service");
            }
            else if(Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            }*/

            //stopSelf();
        }
    };


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
}
