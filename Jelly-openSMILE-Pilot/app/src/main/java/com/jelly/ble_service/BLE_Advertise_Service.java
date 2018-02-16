package com.jelly.ble_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jelly.constant.Constants;

import java.util.concurrent.TimeUnit;


/**
 * Created by tiantianfeng on 10/13/17.
 */

public class BLE_Advertise_Service extends Service {

    private BluetoothAdapter bluetoothAdapter;

    private String DEBUG                = "TILEs";
    private String BLE_MANAGER_NULL     = "No BLE Manager";
    private String BLE_ADV_NULL         = "No BLE Advertiser";

    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;

    public static final String ADVERTISING_FAILED =
            "com.example.android.bluetoothadvertisements.advertising_failed";

    private final int ENABLE_SCAN       = 1;
    private final int ENABLE_ADVERTISE  = 2;

    /**
     * Length of time to allow advertising before automatically shutting off. (2 minutes)
     */
    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(15, TimeUnit.SECONDS);

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 45;

    private int DEBUG_STATE;
    private String jellyTokenID;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        DEBUG_STATE = ENABLE_ADVERTISE;

        writeSharedPreference(Constants.BLE_STATUS, Constants.BLE_ON);
        Log.d(Constants.DEBUG, "Start BT Advertisement Service");

        if(retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED) && isAllPermissionGranted()) {

            initialize();

            jellyTokenID = retrieveSharedPreference(Constants.QR_CODE_ID).substring(0, 4);

            Log.d(DEBUG, "Start BT Advertisement Service");
            startAdvertising();
            setTimeout();

        } else {
            setTimeout();
        }

    }

    private boolean isAllPermissionGranted() {
        if(retrieveSharedPreference(Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /**
         * Note that onDestroy is not guaranteed to be called quickly or at all. Services exist at
         * the whim of the system, and onDestroy can be delayed or skipped entirely if memory need
         * is critical.
         * */
        if (isAllPermissionGranted()) {
            switch (DEBUG_STATE) {
                case ENABLE_ADVERTISE:
                    stopAdvertising();
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    break;

                case ENABLE_SCAN:

                    break;

                case (ENABLE_ADVERTISE | ENABLE_SCAN) :
                    stopAdvertising();
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    break;

                default:
                    break;

            }
        }

    }

    /**
     * Stops BLE Advertising.
     */
    private void stopAdvertising() {
        Log.d(DEBUG, "Service: Stopping Advertising");
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
        }


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager != null) {
                bluetoothAdapter = mBluetoothManager.getAdapter();

                if (bluetoothAdapter != null) {
                    if(bluetoothAdapter.isEnabled()) {
                        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                    } else {
                        bluetoothAdapter.enable();
                        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                    }
                } else {
                    Log.d(DEBUG, BLE_ADV_NULL);
                }

            } else {
                Log.d(DEBUG, BLE_MANAGER_NULL);
            }

        }

    }

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {

        Log.d(DEBUG, "Service: Starting Advertising");

        if (mAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            mAdvertiseCallback = new SampleAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                mBluetoothLeAdvertiser.startAdvertising(settings, data,
                        mAdvertiseCallback);
            }
        }

    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        settingsBuilder.setConnectable(false);

        return settingsBuilder.build();
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class SampleAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Log.d(DEBUG, Integer.toString(errorCode));
            Log.d(DEBUG, "Advertising failed");
            stopSelf();
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(DEBUG, "Advertising successfully started");
        }
    }


    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */
        Log.d(DEBUG, jellyTokenID);

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(Constants.Service_UUID);
        dataBuilder.setIncludeDeviceName(true);

        dataBuilder.addServiceData(Constants.Service_UUID, jellyTokenID.getBytes());

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

        return dataBuilder.build();
    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout(){
        timeoutHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(Constants.DEBUG, "AdvertiserService has reached timeout of "+TIMEOUT+" milliseconds, stopping advertising.");
                scheduleNextService();
                stopSelf();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT);
    }


    private void scheduleNextService() {
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent bleAdvertiseIntent = new Intent(getApplicationContext(), BLE_Advertise_Service.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, bleAdvertiseIntent, PendingIntent.FLAG_ONE_SHOT);

        if(Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            Log.d("Tiles", "Set BLE Advertise Alarm Service");
        }
        else if(Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
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


}
