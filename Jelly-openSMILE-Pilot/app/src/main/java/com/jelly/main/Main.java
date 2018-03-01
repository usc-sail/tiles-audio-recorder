package com.jelly.main;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.audeering.opensmile.androidtemplate.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.jelly.battery.Battery_Service;
import com.jelly.constant.Constants;
import com.jelly.network.File_Uploading_Service;
import com.jelly.network.Upload_Battery_Info;
import com.jelly.off_boarding.ReadParagraphActivity;
import com.jelly.opensmile.OpenSmile_Service;
import com.jelly.opensmile.OpenSmile_VAD;
import com.jelly.opensmile_debug.OpenSmile_Debug;
import com.jelly.qr.QR_Code_Activity;
import com.mvp.presenter.JellyTokenPresenter;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by tiantianfeng on 10/18/17.
 */

public class Main extends AppCompatActivity {

    private PendingIntent pendingIntent;

    public static final long ALARM_TRIGGER_AT_TIME = SystemClock.elapsedRealtime() + 10000;

    private AlarmManager alarmManager;

    /* Debug Parameters **/
    private final int ENABLE_BT             = 1;
    private final int ENABLE_DISCOVERABLE   = 2;
    private final int ENABLE_RECORD         = 3;
    private final int ENABLE_POCKET         = 4;
    private final int ENABLE_BLE            = 5;
    private final int ENABLE_OPENSMILE      = 6;
    private final int ENABLE_OPENSMILE_VAD  = 7;
    private final int ENABLE_DEBUG_CONFIG   = 8;
    private final int ENABLE_DEFAULT        = 99;

    private String DEBUG                = "TILEs";

    private final int MY_PERMISSIONS_REQUEST    = 100;

    private PowerManager.WakeLock wakeLock;

    private final int BLUETOOTH                 = 1;
    private final int BATTERY_STATS             = 2;
    private final int BLUETOOTH_ADMIN           = 3;
    private final int BLUETOOTH_PRIVILEGED      = 4;
    private final int READ_EXTERNAL_STORAGE     = 5;
    private final int WRITE_EXTERNAL_STORAGE    = 6;
    private final int SET_ALARM                 = 7;
    private final int WAKE_LOCK                 = 8;
    private final int ACCESS_COARSE_LOCATION    = 9;
    private final int ACCESS_FINE_LOCATION      = 10;
    private final int INTERNET                  = 11;
    private final int ACCESS_WIFI_STATE         = 12;
    private final int ACCESS_NETWORK_STATE      = 13;
    private final int RECEIVE_BOOT_COMPLETED    = 14;

    private final String[] permissionName = {Manifest.permission.BLUETOOTH,
                                                Manifest.permission.BATTERY_STATS,
                                                Manifest.permission.BLUETOOTH_ADMIN,
                                                Manifest.permission.BLUETOOTH_PRIVILEGED,
                                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.SET_ALARM, Manifest.permission.WAKE_LOCK,
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET,
                                                Manifest.permission.ACCESS_WIFI_STATE,
                                                Manifest.permission.CHANGE_WIFI_STATE,
                                                Manifest.permission.ACCESS_NETWORK_STATE,
                                                Manifest.permission.RECEIVE_BOOT_COMPLETED};

    private final String[] permissionMainName = { Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA};

    private ArrayList<String> ungrantPermission;
    private ArrayList<Integer> ungrantPermissionCode;
    private int ungrantPermissionIndex = 0;

    /*
    *   UI Related
    * */
    private TextView Running_Status_Textview;
    private Button TurnOnOffButton, UploadDataButton;
    private TextView FileSize_Textview;

    private Handler mHandler = new Handler();
    private Handler wifiHandler = new Handler();
    private Handler enableButtonHandler = new Handler();
    private boolean isHandlerRun = false;

    private JellyTokenPresenter mJellyTokenPresenter;

    /*
    *   GPS Function(Not work as expected)
    * */
    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;

    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(Main.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            Log.d("Location error", "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(Main.this, REQUEST_LOCATION);

                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ungrantPermission       = new ArrayList<String>();
        ungrantPermissionCode   = new ArrayList<Integer>();

        for (int i = 0; i < permissionName.length; i++) {

            if(!checkPermission(permissionName[i])) {
                ungrantPermission.add(permissionName[i]);
                ungrantPermissionCode.add(i + 1);
            }
        }

        if(ungrantPermission.size() > 0) {
            Log.d(DEBUG, "Request" + ungrantPermission.get(0));
            requestPermission(ungrantPermission.get(0), ungrantPermissionCode.get(0));
            ungrantPermissionIndex = ungrantPermissionIndex + 1;
        }

        isIgnoreBatteryOption(Main.this);

        initUI();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);//Menu Resource, Menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_qr_code:

                if(isAllPermissionGranted()) {
                    Toast.makeText(getApplicationContext(),"SCAN QR CODE", Toast.LENGTH_LONG).show();
                    Intent qr_code_activity = new Intent(this, QR_Code_Activity.class);
                    startActivityForResult(qr_code_activity, Constants.QR_CODE_SCAN_REQUEST);
                    return true;
                } else {
                    Toast.makeText(getApplicationContext(),"Please Enable All the Permissions First", Toast.LENGTH_LONG).show();
                }


            case R.id.sync_tile:
                if(retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED) && isAllPermissionGranted()){
                    if(checkWIFIAvailability() && retrieveSharedPreference(Constants.QR_CODE_SYNC).equals(Constants.QR_CODE_UN_SYNCED)) {
                        Toast.makeText(this, "Try to Sync to Server", Toast.LENGTH_SHORT).show();
                        syncJellyToken(retrieveSharedPreference(Constants.QR_CODE_ID));
                    } else if (retrieveSharedPreference(Constants.QR_CODE_SYNC).equals(Constants.QR_CODE_SYNCED)){
                        Toast.makeText(this, "Already Synced", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(this, "No available WIFI", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(this, "No Jelly Token has been scanned, " +
                            "please scan the QR code from TILES app first", Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.enable_permission:
                RequestMultiplePermission();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == Constants.QR_CODE_SCAN_REQUEST) {
            if(resultCode == Activity.RESULT_OK && intent != null){
                String result = intent.getStringExtra(Constants.QR_CODE_SCAN_EXTRA);
                String[] urlParts = result.split("/");
                String jellyTokenID = urlParts[urlParts.length - 1];

                writeSharedPreference(Constants.QR_CODE_SCANNED, Constants.QR_CODE_IS_SCANNED);
                writeSharedPreference(Constants.QR_CODE_ID, jellyTokenID);


                if(checkWIFIAvailability()) {

                    Toast.makeText(this, "Try to Sync to Server", Toast.LENGTH_SHORT).show();
                    Log.d(DEBUG, "Contents = " + jellyTokenID);
                    writeSharedPreference(Constants.QR_CODE_SYNC, Constants.QR_CODE_UN_SYNCED);

                    syncJellyToken(jellyTokenID);

                    Timer t = new Timer();
                    t.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            if(retrieveSharedPreference(Constants.QR_CODE_SYNC).equals(Constants.QR_CODE_SYNCED)) {
                                Toast.makeText(getApplicationContext(), "Sync Complete", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Sync Failed! Please retry.", Toast.LENGTH_SHORT).show();
                            }
                            Looper.loop();
                        }
                    }, 6 * 1000);


                } else {

                    Toast.makeText(this, "No available WIFI", Toast.LENGTH_SHORT).show();
                    Log.d(DEBUG, "Contents = " + jellyTokenID);

                }

            }
        }

    }

    private boolean checkWIFIAvailability() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    private void syncJellyToken(String jellyTokenID) {
        Intent jellyTokenPresenter = new Intent(this, JellyTokenPresenter.class);
        startService(jellyTokenPresenter);
    }

    private String retrieveSharedPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(key, "");
    }

    private void initUI() {

        setContentView(R.layout.main);
        Running_Status_Textview     = (TextView) findViewById(R.id.status_textView);
        TurnOnOffButton             = (Button) findViewById(R.id.turn_on_off_btn);
        UploadDataButton            = (Button) findViewById(R.id.upload_data_btn);
        FileSize_Textview           = (TextView) findViewById(R.id.filesize_textView);

        initButton();
        initFileSize();

        TurnOnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isAllPermissionGranted()) {

                    if(retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED)) {
                        if(retrieveSharedPreference(Constants.VAD_ON_OFF).contains(Constants.VAD_OFF)) {

                            writeSharedPreference(Constants.VAD_ON_OFF, Constants.VAD_ON);

                            if(isAllPermissionGranted()) {
                                Running_Status_Textview.setVisibility(View.VISIBLE);
                                Running_Status_Textview.setText("Not Available");
                            } else {
                                Running_Status_Textview.setVisibility(View.INVISIBLE);
                            }

                            TurnOnOffButton.setText("Audio is On");
                            TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOn));

                        } else {

                            writeSharedPreference(Constants.VAD_ON_OFF, Constants.VAD_OFF);
                            writeSharedPreference(Constants.VAD_OFF_TIME, "0");
                            Running_Status_Textview.setVisibility(View.INVISIBLE);

                            TurnOnOffButton.setText("Audio is Off");
                            TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOff));

                        }

                    } else {
                        if(isAllPermissionGranted()) {
                            Toast.makeText(getApplicationContext(),"SCAN QR CODE", Toast.LENGTH_LONG).show();
                            Intent qr_code_activity = new Intent(getApplicationContext(), QR_Code_Activity.class);
                            startActivityForResult(qr_code_activity, Constants.QR_CODE_SCAN_REQUEST);
                            Toast.makeText(getApplicationContext(),"Please scan the QR code First", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(),"Please Enable All the Permissions First", Toast.LENGTH_LONG).show();
                        }
                    }

                } else {
                    Toast.makeText(getApplicationContext(),"Please Enable the Permission First", Toast.LENGTH_LONG).show();
                    RequestMultiplePermission();

                }
            }
        });

        UploadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isAllPermissionGranted()) {
                    if(retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED)) {
                        if(retrieveSharedPreference(Constants.UPLOAD_STATUS).contains(Constants.UPLOAD_ON)) {

                            Toast.makeText(getApplicationContext(),"Turn Off Uploading", Toast.LENGTH_LONG).show();

                            WifiManager mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            mainWifi.setWifiEnabled(false);

                            UploadDataButton.setText("UPLOAD DATA");
                            UploadDataButton.setEnabled(false);
                            UploadDataButton.setBackgroundColor(getResources().getColor(R.color.colorOff));
                            writeSharedPreference(Constants.UPLOAD_STATUS, Constants.UPLOAD_OFF);
                            enableButtonHandler.postDelayed(enableButtonRunnable, 8 * 1000);

                        } else {
                            WifiManager mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            if (mainWifi.isWifiEnabled() == false)
                            {
                                Log.d(Constants.DEBUG, "Main->UploadDataButton->enable wifi");
                                mainWifi.setWifiEnabled(true);
                                UploadDataButton.setEnabled(false);
                                Toast.makeText(getApplicationContext(),"Open WIFI", Toast.LENGTH_LONG).show();
                                wifiHandler.postDelayed(wifiRunnable, 15 * 1000);

                            } else {
                                if(checkWIFIAvailability()) {
                                    startBatteryUploadingService();
                                    startFileUploadingService();
                                    UploadDataButton.setText("PAUSE");
                                    UploadDataButton.setBackgroundColor(getResources().getColor(R.color.colorOn));
                                    writeSharedPreference(Constants.UPLOAD_STATUS, Constants.UPLOAD_ON);
                                    Toast.makeText(getApplicationContext(),"Start Upload", Toast.LENGTH_LONG).show();
                                } else {
                                    UploadDataButton.setText("UPLOAD DATA");
                                    UploadDataButton.setBackgroundColor(getResources().getColor(R.color.colorOff));
                                    writeSharedPreference(Constants.UPLOAD_STATUS, Constants.UPLOAD_OFF);
                                    Toast.makeText(getApplicationContext(),"Please Connect to WIFI first", Toast.LENGTH_LONG).show();
                                }
                            }

                        }
                    } else {
                        Toast.makeText(getApplicationContext(),"Please scan the QR code First", Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(getApplicationContext(),"Please Enable the Permission First", Toast.LENGTH_LONG).show();
                }

            }
        });

    }

    private Runnable wifiRunnable = new Runnable() {
        @Override
        public void run() {
            if(checkWIFIAvailability()) {
                startBatteryUploadingService();
                startFileUploadingService();
                UploadDataButton.setText("PAUSE");
                UploadDataButton.setEnabled(true);
                UploadDataButton.setBackgroundColor(getResources().getColor(R.color.colorOn));
                writeSharedPreference(Constants.UPLOAD_STATUS, Constants.UPLOAD_ON);
                Toast.makeText(getApplicationContext(),"Start Upload", Toast.LENGTH_LONG).show();
            } else {
                UploadDataButton.setText("UPLOAD DATA");
                UploadDataButton.setEnabled(true);
                UploadDataButton.setBackgroundColor(getResources().getColor(R.color.colorOff));
                writeSharedPreference(Constants.UPLOAD_STATUS, Constants.UPLOAD_OFF);
                Toast.makeText(getApplicationContext(),"Please Connect to WIFI first", Toast.LENGTH_LONG).show();
            }
        }
    };

    private Runnable enableButtonRunnable = new Runnable() {
        @Override
        public void run() {
            UploadDataButton.setEnabled(true);
            Toast.makeText(getApplicationContext(),"Upload has been cancelled", Toast.LENGTH_LONG).show();
        }
    };

    private void initButton() {

        if(isAllPermissionGranted()) {
            if(retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED)) {
                if(retrieveSharedPreference(Constants.VAD_ON_OFF).isEmpty()) {
                    writeSharedPreference(Constants.VAD_ON_OFF, Constants.VAD_ON);
                    writeSharedPreference(Constants.VAD_OFF_TIME, "0");
                    TurnOnOffButton.setText("Audio is On");
                    TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOn));
                } else if (retrieveSharedPreference(Constants.VAD_ON_OFF).contains(Constants.VAD_ON)) {
                    TurnOnOffButton.setText("Audio is On");
                    TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOn));
                } else if (retrieveSharedPreference(Constants.VAD_ON_OFF).contains(Constants.VAD_OFF)) {
                    TurnOnOffButton.setText("Audio is Off");
                    TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorOff));
                }
            } else {
                TurnOnOffButton.setText("Scan QR Code");
                TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }
        } else {
            TurnOnOffButton.setText("Enable Permission");
            TurnOnOffButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

    }

    private void initSharedPreference() {
        if(retrieveSharedPreference(Constants.QR_CODE_SCANNED).isEmpty()){
            writeSharedPreference(Constants.QR_CODE_SCANNED, Constants.QR_CODE_NOT_SCANNED);
            writeSharedPreference(Constants.QR_CODE_SYNC, Constants.QR_CODE_UN_SYNCED);
        }

        if(retrieveSharedPreference(Constants.VAD_OFF_TIME).isEmpty()) {
            writeSharedPreference(Constants.VAD_OFF_TIME, "0");
        }

        if(retrieveSharedPreference(Constants.VAD_ON_OFF).isEmpty()) {
            writeSharedPreference(Constants.VAD_OFF, "0");
        }

        if(retrieveSharedPreference(Constants.VAD_INVALID_TIME).isEmpty()) {
            writeSharedPreference(Constants.VAD_INVALID_TIME, "0");
        }

        if(retrieveSharedPreference(Constants.WORK_WIFI).isEmpty()) {
            writeSharedPreference(Constants.WORK_WIFI, Constants.WORK_WIFI_ON);
        }

        if(retrieveSharedPreference(Constants.WORK_WIFI_OFF_TIME).isEmpty()) {
            writeSharedPreference(Constants.WORK_WIFI_OFF_TIME, "0");
        }

        if(retrieveSharedPreference(Constants.UPLOAD_STATUS).isEmpty()) {
            writeSharedPreference(Constants.UPLOAD_STATUS, Constants.UPLOAD_OFF);
        }

        if(retrieveSharedPreference(Constants.BLE_STATUS).isEmpty()) {
            writeSharedPreference(Constants.BLE_STATUS, Constants.BLE_OFF);
        }

        if(retrieveSharedPreference(permissionMainName[0]).isEmpty()) {
            for(int i = 0; i < 4; i++) {
                writeSharedPreference(permissionMainName[i], Integer.toString(Constants.PER_DISABLE));
            }
        }

        if(retrieveSharedPreference(Constants.PER_STATUS).isEmpty()) {
            writeSharedPreference(Constants.PER_STATUS, Constants.PER_NOT_ALL_GRANTED);
        }

    }

    private void init() {

        enPower();
        writeSharedPreference(Constants.VAD_WRITE, Constants.FALSE);
        writeSharedPreference(Constants.VAD_STATUS, Constants.VAD_INIT);

        initSharedPreference();
        initFileSize();

        int DEBUG_MODE = ENABLE_OPENSMILE_VAD;

        switch (DEBUG_MODE) {

            case ENABLE_OPENSMILE:
                startOpenSmileService();
                break;

            case ENABLE_OPENSMILE_VAD:
                break;

            case ENABLE_DEBUG_CONFIG:
                startDebugOpenSmileConfService();
                break;

            default:
                break;
        }

        /*
        *  Always enable Battery logger to csv
        * */
        startBatteryService();

    }

    @Override
    protected void onResume() {
        super.onResume();

        initButton();
        updateGUI();
        initFileSize();

        if(!isHandlerRun) {
            mHandler.postDelayed(mTickExecutor, 2500);
            isHandlerRun = true;
        }

    }

    private void initFileSize() {
        if(retrieveSharedPreference(Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)){
            countFileSize();
        }

        if(retrieveSharedPreference(Constants.FILE_SIZE).isEmpty()) {
            writeSharedPreference(Constants.FILE_SIZE, "0");
        }
        FileSize_Textview.setText(retrieveSharedPreference(Constants.FILE_SIZE));
    }

    private void updateGUI() {



        if(retrieveSharedPreference(Constants.VAD_ON_OFF).equals(Constants.VAD_ON)) {

            if(isAllPermissionGranted() && retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED)) {
                String jellyTokenID = retrieveSharedPreference(Constants.QR_CODE_ID).substring(0, 4);

                if(retrieveSharedPreference(Constants.VAD_CURRENT_ON).contains(Constants.VAD_ON)) {
                    Running_Status_Textview.setVisibility(View.VISIBLE);
                    Running_Status_Textview.setText("Your User ID: " + jellyTokenID + "\nVAD CURRENTLY RUNNING");
                } else if (retrieveSharedPreference(Constants.VAD_CURRENT_ON).contains(Constants.VAD_IDLE)) {
                    Running_Status_Textview.setVisibility(View.VISIBLE);
                    Running_Status_Textview.setText("Your User ID: " + jellyTokenID + "\nVAD CURRENTLY IDLE");
                } else if (retrieveSharedPreference(Constants.OPENSMILE_CURRENT_ON).contains(Constants.VAD_ON)) {
                    Running_Status_Textview.setVisibility(View.VISIBLE);
                    Running_Status_Textview.setText("Your User ID: " + jellyTokenID + "\nOPENSMILE CURRENTLY RUNNING");
                }
            } else {
                Running_Status_Textview.setVisibility(View.INVISIBLE);
            }

        } else {
            if(retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED)) {
                String jellyTokenID = retrieveSharedPreference(Constants.QR_CODE_ID).substring(0, 4);
                int time_remaining = 30 - Integer.parseInt(retrieveSharedPreference(Constants.VAD_OFF_TIME)) * 5;
                Running_Status_Textview.setVisibility(View.VISIBLE);
                Running_Status_Textview.setText("Your User ID:" + jellyTokenID + "\nACOUSTIC WILL BE RESUME BACK IN " + Integer.toString(time_remaining) + " MIN");
            } else {
                Running_Status_Textview.setVisibility(View.INVISIBLE);
            }
        }

        if(retrieveSharedPreference(Constants.UPLOAD_STATUS).contains(Constants.UPLOAD_ON)){
            UploadDataButton.setText("PAUSE");
            UploadDataButton.setBackgroundColor(getResources().getColor(R.color.colorOn));
        } else {
            UploadDataButton.setText("UPLOAD DATA");
            UploadDataButton.setBackgroundColor(getResources().getColor(R.color.colorOff));
        }

    }

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
        updateGUI();
        initButton();
        initFileSize();
        mHandler.postDelayed(mTickExecutor, 2500);
        isHandlerRun = true;
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(isHandlerRun) {
            mHandler.removeCallbacks(mTickExecutor);
            isHandlerRun = false;
        }
    }

    private void enPower() {

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyWakelockTag");
        wakeLock.acquire();

        Log.d("TILES", Integer.toString(Build.VERSION.SDK_INT));

    }

    private void startOpenSmileService() {

        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent openSMILE_Intent = new Intent(this, OpenSmile_Service.class);
        pendingIntent = PendingIntent.getService(Main.this, 1, openSMILE_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

    }

    private void startDebugOpenSmileConfService() {

        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent openSMILE_Debug_Intent = new Intent(this, OpenSmile_Debug.class);
        pendingIntent = PendingIntent.getService(Main.this, 1, openSMILE_Debug_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

    }

    private void startOpenSmileVADService() {

        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent openSMILE_Intent = new Intent(this, OpenSmile_VAD.class);
        pendingIntent = PendingIntent.getService(Main.this, 1, openSMILE_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

    }

    private void startBatteryService() {

        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent battery_Intent = new Intent(this, Battery_Service.class);
        pendingIntent = PendingIntent.getService(Main.this, 1, battery_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ALARM_TRIGGER_AT_TIME, pendingIntent);

    }

    private void startFileUploadingService() {

        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent fileUploading_Intent = new Intent(this, File_Uploading_Service.class);
        pendingIntent = PendingIntent.getService(Main.this, 1, fileUploading_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5000, pendingIntent);

    }

    private void startBatteryUploadingService() {

        /*
        *   Repeat the recording services every 3min (It will vary according to test results)
        */
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);

        Intent fileUploading_Intent = new Intent(this, Upload_Battery_Info.class);
        pendingIntent = PendingIntent.getService(Main.this, 1, fileUploading_Intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        *   Alarm set repeat is not exact and can have significant drift
        * */
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 2000, pendingIntent);

    }

    private void startReadParagraphActivity() {
        Intent read_Para_activity = new Intent(this, ReadParagraphActivity.class);
        startActivityForResult(read_Para_activity, Constants.QR_CODE_SCAN_REQUEST);
    }

    private boolean checkPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        else {

            Log.d("TILES", permission + " granted");
            return true;
        }
    }

    private void requestPermission(String permission, int requestCode) {


        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission)) {
                Toast.makeText (this,
                        permission,
                        Toast.LENGTH_LONG).show ();

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{permission},
                        requestCode);

            }


        } else {

            Log.d("TILES", permission + " granted");
        }

    }

    private void RequestMultiplePermission() {

        ActivityCompat.requestPermissions(Main.this, permissionMainName, MY_PERMISSIONS_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TILES", "Granted");

                } else if (grantResults.length > 0) {

                    Log.d("Debug", "Not grant");
                }

                for (int i = 0; i < permissions.length; i++) {
                    writeSharedPreference(permissions[i], Integer.toString(grantResults[i]));
                }

                int numberOfGrantedPermissions = 0;
                for(int i = 0; i < 4; i++) {
                    Log.d(Constants.DEBUG, retrieveSharedPreference(permissionMainName[i]));
                    if(retrieveSharedPreference(permissionMainName[i]).equals(Integer.toString(Constants.PER_ENABLE))) {
                        numberOfGrantedPermissions = numberOfGrantedPermissions + 1;
                    }
                }

                Log.d(Constants.DEBUG, "Main->onRequestPermissionsResult->" + numberOfGrantedPermissions);
                if(numberOfGrantedPermissions == 4) {
                    writeSharedPreference(Constants.PER_STATUS, Constants.PER_ALL_GRANTED);
                }


                Log.d(DEBUG, "Main->onRequestPermissionsResult");

                ungrantPermission       = new ArrayList<String>();
                ungrantPermissionCode   = new ArrayList<Integer>();

                for (int i = 0; i < permissionName.length; i++) {

                    if(!checkPermission(permissionName[i])) {
                        ungrantPermission.add(permissionName[i]);
                        ungrantPermissionCode.add(i + 1);
                    }
                }

                if(ungrantPermission.size() > 0) {
                    Log.d(DEBUG, "Request" + ungrantPermission.get(0));
                    requestPermission(ungrantPermission.get(0), ungrantPermissionCode.get(0));
                    ungrantPermissionIndex = ungrantPermissionIndex + 1;
                }

                return;
            }
        }


    }

    public static void isIgnoreBatteryOption(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivityForResult(intent, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void ignoreOptimizationApp() {
        List<PackageInfo> apps = getPackageManager().getInstalledPackages(0);
        ArrayList<String> notOptimizedList = new ArrayList<String>();

        for(int i = 0; i < apps.size(); i++) {

            PackageInfo packageInfo = apps.get(i);

            String packageName = packageInfo.packageName;
            //Log.d(DEBUG, packageName);

            if(packageName.contains("com.android.bluetooth") || packageName.contains("com.android.bluetoothmidiservice") ||
                    packageName.contains("com.google.android.deskclock") || packageName.contains("Android Shared Library") ||
                    packageName.contains("com.google.android.launcher") || packageName.contains("com.android.providers.calendar") ||
                    packageName.contains("com.android.providers.media") || packageName.equals("android")) {

                notOptimizedList.add(packageName);
                Log.d(DEBUG, packageName);
            }

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            for(int i = 0; i < notOptimizedList.size(); i++) {

                String packageName = notOptimizedList.get(i);

                try {
                    Intent intent = new Intent();
                    PowerManager pm = (PowerManager) getApplication().getSystemService(Context.POWER_SERVICE);
                    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + packageName));
                        this.startActivityForResult(intent, 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }


    }

    private void writeSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void countFileSize() {
        String mainPath = "TILEs";
        File[] files;
        List<Date> dateList = new ArrayList<>();
        SimpleDateFormat dateFormat;

        int fileSize = 0;

        dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        File mainFile = new File(Environment.getExternalStorageDirectory(), mainPath);
        if (!mainFile.exists()) {
            if (!mainFile.mkdirs()) {
                Log.e(DEBUG, "Main->Problem creating folder");
            }
        }
        if(mainFile.exists()) {
            files = mainFile.listFiles();
            if (files.length > 0) {
                for (File file : files) {
                    if (file.getName().contains("-")) {
                        try {
                            Date tempDate = dateFormat.parse(file.getName());
                            dateList.add(tempDate);
                        } catch (ParseException e) {
                        }
                    }
                }

                Collections.sort(dateList);

                for(int i = 0; i < dateList.size(); i++) {
                    File csvFile = new File(Environment.getExternalStorageDirectory(),
                            mainPath + "/" + dateFormat.format(dateList.get(i)).toString() + "/" + "csv");
                    if(csvFile.exists()) {
                        files = csvFile.listFiles();
                        if (files.length > 0) {
                            fileSize = fileSize + files.length;
                        }
                    }
                }
                Log.d(DEBUG, "Main->countFileSize->Size: "+ fileSize);
            }
        }

        writeSharedPreference(Constants.FILE_SIZE, Integer.toString(fileSize));
    }

    private boolean isAllPermissionGranted() {

        int numberOfGrantedPermissions = 0;
        for(int i = 0; i < 4; i++) {
            if(retrieveSharedPreference(permissionMainName[i]).equals(Integer.toString(Constants.PER_ENABLE))) {
                numberOfGrantedPermissions = numberOfGrantedPermissions + 1;
            }
        }
        if(numberOfGrantedPermissions == 4) {
            writeSharedPreference(Constants.PER_STATUS, Constants.PER_ALL_GRANTED);
        }

        if(retrieveSharedPreference(Constants.PER_STATUS).equals(Constants.PER_ALL_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }
}



