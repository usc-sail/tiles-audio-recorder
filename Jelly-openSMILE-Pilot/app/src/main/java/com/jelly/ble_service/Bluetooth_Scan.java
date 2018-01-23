package com.jelly.ble_service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by tiantianfeng on 9/14/17.
 */


public class Bluetooth_Scan extends Service {

    private BluetoothAdapter bluetoothAdapter;

    private ArrayList<String> bluetoothDeviceName       = new ArrayList<String>();
    private ArrayList<String> bluetoothDeviceRSSI       = new ArrayList<String>();
    private ArrayList<String> bluetoothDeviceFNDTime    = new ArrayList<String>();

    private boolean isBluetoothFound = false;

    private final int CSV_DATA_ENTRY_SIZE   = 3;
    private final int BT_FND_TIME_INDEX     = 0;
    private final int BT_DEVICE_NAME_INDEX  = 1;
    private final int BT_DEVICE_RSSI_INDEX  = 2;

    private final int MY_PERMISSIONS_REQUEST    = 1;

    /*
    *   Alarm Manager
    * */
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    public static final long ALARM_INTERVAL = 1000 * 60;

    private String DEBUG        = "TILEs";
    private String DEBUG_FILE   = "TILEs_File";

    private int BT_SCAN_DETECTION_DURATION = 60000;
    private Handler btscanHandler = new Handler();

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        Log.d(DEBUG, "Start BT service");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothAdapter.getScanMode();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.enable();

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
        bluetoothAdapter.startDiscovery();

        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        Intent record_Intent = new Intent(getApplicationContext(), Bluetooth_Scan.class);
        pendingIntent = PendingIntent.getService(getApplicationContext(), 1, record_Intent, PendingIntent.FLAG_ONE_SHOT);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ALARM_INTERVAL, pendingIntent);

        btscanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                unregisterReceiver(mReceiver);
                saveDataToCSV(isBluetoothFound);
                stopSelf();

            }
        }, BT_SCAN_DETECTION_DURATION);

        /*
        boolean asyncTest = true;


        if(asyncTest) {

            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    PermissionResponse response = null;
                    try {
                        response = PermissionEverywhere.getPermission(getApplicationContext(),
                                new String[]{Manifest.permission.SET_ALARM},
                                123, "Tile", "This app needs a write permission", R.mipmap.ic_launcher).call();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    boolean isGranted = response.isGranted();

                    if(isGranted) {
                        Log.d(DEBUG, "Granted: ");

                    }

                    return isGranted;
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    super.onPostExecute(aBoolean);


                }
            }.execute();


        } else {

            PermissionEverywhere.getPermission(getApplicationContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BATTERY_STATS,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_PRIVILEGED,
                            Manifest.permission.SET_ALARM},
                    123, "My Awesome App", "This app needs a permission", R.mipmap.ic_launcher).enqueue(new PermissionResultCallback() {
                @Override
                public void onComplete(PermissionResponse permissionResponse) {


                }
            });
        }*/

    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

                /* Discovery starts, we can show progress dialog or perform other tasks **/

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                /* Discovery finishes, dismis progress dialog **/
                Log.d(DEBUG, "Scan Finished!");


            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                /* Bluetooth device found & Add device Info **/
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int             rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                Calendar calendar = Calendar.getInstance();

                String time = Integer.toString(calendar.get(Calendar.MONTH) + 1) + "-" + Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))
                                + "-" + Integer.toString(calendar.get(Calendar.YEAR))   + "-" + Integer.toString(calendar.get(Calendar.HOUR))
                                + "-" + Integer.toString(calendar.get(Calendar.MINUTE)) + "-" + Integer.toString(calendar.get(Calendar.SECOND));

                if (device.getName() != "")
                {
                    if(device.getName().contains("Jelly")) {
                        bluetoothDeviceName.add(device.getName());
                        bluetoothDeviceRSSI.add(Integer.toString(rssi));
                        bluetoothDeviceFNDTime.add(time);
                    }

                }
                Log.d(DEBUG, time + ":" + device.getName() + ": " + device.getAddress() + Integer.toString(rssi));

                isBluetoothFound = true;
            }
        }
    };

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
    public void onDestroy() {
        super.onDestroy();
    }

    private void saveDataToCSV(boolean isBTFound) {

        if (isBTFound) {

            String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/BT_Test.csv";

            try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true))) {

                for (int i = 0; i < bluetoothDeviceName.size(); i++) {

                    String[] data = new String[CSV_DATA_ENTRY_SIZE];

                    data[BT_FND_TIME_INDEX]     = bluetoothDeviceFNDTime.get(i);
                    data[BT_DEVICE_NAME_INDEX]  = bluetoothDeviceName.get(i);
                    data[BT_DEVICE_RSSI_INDEX]  = bluetoothDeviceRSSI.get(i);

                    writer.writeNext(data);
                }

                writer.close();

            } catch (IOException e) {

            }



            try (CSVReader reader = new CSVReader(new FileReader(filepath), ',');) {

                List<String[]> rows = reader.readAll();

                for (String[] row: rows) {

                    Log.d(DEBUG_FILE, Arrays.toString(row));

                }
            } catch (IOException e) {

            }




        }

    }
}
