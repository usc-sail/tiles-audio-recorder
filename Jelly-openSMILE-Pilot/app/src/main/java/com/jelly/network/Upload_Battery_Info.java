package com.jelly.network;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.jelly.constant.Constants;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by tiantianfeng on 2/10/18.
 */

public class Upload_Battery_Info extends Service {


    private final String DEBUG = "TILEs";

    private AmazonS3Client s3Client;
    private NotificationManager nm;

    Map<Integer, String> map = new HashMap<>();
    private int currentFileIndex = 0;
    private File[] files;
    private TransferListener listener = new UploadListener();

    private String jellyTokenID, uploadFileDirectory;

    private int numOfErr = 0;

    private TransferObserver observer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(startId > 0) {
            stopSelfResult(startId);
            Log.d(Constants.DEBUG, "Upload_Battery_Service->onStartCommand->stopSelfResult");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (isDeviceOnline() && retrieveSharedPreference(Constants.QR_CODE_SCANNED).equals(Constants.QR_CODE_IS_SCANNED)) {

            jellyTokenID = retrieveSharedPreference(Constants.QR_CODE_ID);
            Log.d(Constants.DEBUG, "File_Uploading_Service->onStart->jellyTokenID:" + jellyTokenID);

            ClientConfiguration clientConfiguration = new ClientConfiguration();

            clientConfiguration.setMaxErrorRetry(3);
            clientConfiguration.setMaxConnections(8);
            clientConfiguration.setConnectionTimeout(15 * 1000);

            /*
            *   Init Amazon Client Object and Transfer object
            * */
            s3Client = new AmazonS3Client(new BasicAWSCredentials(Constants.S3_ACCESS_KEY, Constants.S3_SECRET));
            s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));
            //s3Client.setRegion(Region.getRegion(Regions.DEFAULT_REGION));

            TransferUtility transferUtility = new TransferUtility(s3Client, getApplicationContext());

            SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String myDate = format.format(new Date());

            String mainPath = "TILEs";
            File mainFile = new File(Environment.getExternalStorageDirectory(), mainPath);
            if (!mainFile.exists()) {
                if (!mainFile.mkdirs()) {
                    Log.e(DEBUG, "File_Uploading_Service->Problem creating folder");
                }
            }

            if(mainFile.exists()) {

                Log.d(DEBUG, "File_Uploading_Service->Transimit folder exist");
                files = mainFile.listFiles();
                Log.d(DEBUG, "File_Uploading_Service->Size: "+ files.length);
                if (files.length > 0) {
                    for (File file : files) {
                        if (file.getName().contains("Battery")) {
                            Log.d(DEBUG, "File_Uploading_Service->File Name: "+ file.getName());

                            uploadFileDirectory = jellyTokenID.substring(0, 4) + "/";
                            Log.d(DEBUG, "File_Uploading_Service->Start Transmitting File->" + uploadFileDirectory);

                            /*
                                    *   Start Transfer
                                    * */
                            observer = transferUtility.upload(
                                    Constants.S3_BUCKET,
                                    uploadFileDirectory + files[currentFileIndex].getName(),
                                    files[currentFileIndex]
                            );

                            // observer.setTransferListener(new );
                            // Sets listeners to in progress transfers
                            if (TransferState.WAITING.equals(observer.getState())
                                    || TransferState.WAITING_FOR_NETWORK.equals(observer.getState())
                                    || TransferState.IN_PROGRESS.equals(observer.getState())) {
                                observer.setTransferListener(listener);
                                map.put(observer.getId(), files[currentFileIndex].getName());
                                Log.d(DEBUG, "observer: " + observer.getId());
                            }
                        }

                    }
                }

            }
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private String retrieveSharedPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(key, "");
    }

    public boolean isDeviceOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void writeSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }


    /*
     * A TransferListener class that can listen to a upload task and be notified
     * when the status changes.
     */
    private class UploadListener implements TransferListener {

        // Simply updates the UI list when notified.
        @Override
        public void onError(int id, Exception e) {
            Log.e(DEBUG, "Error during upload: " + id, e);
            numOfErr = numOfErr + 1;
            stopSelf();
            if(numOfErr > 3) {

            }
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d(DEBUG, String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));

        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            Log.d(DEBUG, "onStateChanged: " + id + ", " + newState);
            if(newState.toString().contains("COMPLETED")) {
                stopSelf();
            } else if (newState.toString().contains("WAITING_FOR_NETWORK")) {
                stopSelf();
            }

        }
    }

}
