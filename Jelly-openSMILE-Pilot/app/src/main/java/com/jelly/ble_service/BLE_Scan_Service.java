package com.jelly.ble_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tiantianfeng on 10/13/17.
 */

public class BLE_Scan_Service extends Service {

    private BluetoothAdapter bluetoothAdapter;

    private String DEBUG                = "TILEs";
    private String BLE_MANAGER_NULL     = "No BLE Manager";
    private String BLE_ADV_NULL         = "No BLE Advertiser";

    private static final long SCAN_PERIOD = 60000;
    private Handler scanHandler = new Handler();

    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;

    public static final String ADVERTISING_FAILED =
            "com.example.android.bluetoothadvertisements.advertising_failed";

    private final int ENABLE_SCAN       = 1;
    private final int ENABLE_ADVERTISE  = 2;

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60;

    private int DEBUG_STATE;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        DEBUG_STATE = ENABLE_SCAN;

        initialize();

        switch (DEBUG_STATE) {

            case ENABLE_SCAN:
                Log.d(DEBUG, "Start BT Scan Service");
                startScanning();
                break;
            default:
                break;

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
        switch (DEBUG_STATE) {

            case ENABLE_SCAN:
                break;

            default:
                break;

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
        if (mBluetoothLeScanner == null) {
            BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager != null) {
                bluetoothAdapter = mBluetoothManager.getAdapter();

                if (bluetoothAdapter != null) {
                    if(bluetoothAdapter.isEnabled()) {
                        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                    } else {
                        bluetoothAdapter.enable();
                        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
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
     * Start scanning for BLE Advertisements (& set it up to stop after a set period of time).
     */
    public void startScanning() {
        if (mScanCallback == null) {
            Log.d(DEBUG, "Starting Scanning");

            scanHandler = new Handler();
            // Will stop the scanning after a set time.
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

            Log.d(DEBUG, "Start to scan");
        } else {
            Log.d(DEBUG, "Failed Init mBluetoothLeScanner");
        }
    }

    /**
     * Stop scanning for BLE Advertisements.
     */
    public void stopScanning() {
        Log.d(DEBUG, "Stopping Scanning");

        // Stop the scan, wipe the callback.
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;

        scheduleNextService();

    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                Log.d(DEBUG, result.toString());
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            Log.d(DEBUG, result.toString());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(DEBUG, "Failed to scan");
        }

    }


    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        // builder.setServiceUuid(Constants.Service_UUID);
        // builder.setDeviceName("Jelly-Pro");
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private void scheduleNextService() {
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent bleScanIntent = new Intent(getApplicationContext(), BLE_Scan_Service.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, bleScanIntent, PendingIntent.FLAG_ONE_SHOT);

        if(Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
            Log.d("Tiles", "Set BLE Scan Alarm Service");
        }
        else if(Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);
        }

    }

}
