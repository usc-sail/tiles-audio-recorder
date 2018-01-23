package com.jelly.application;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.jelly.battery.Battery_Service;
import com.jelly.constant.Constants;
import com.jelly.error_handling.ErrorExceptionHandler;
import com.jelly.main.Main;
import com.jelly.network.File_Uploading_Service;
import com.jelly.opensmile.OpenSmile_Service;
import com.jelly.opensmile.OpenSmile_VAD;
import com.jelly.opensmile_debug.OpenSmile_Debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tiantianfeng on 11/7/17.
 */

public class Jelly_Application extends Application {

    public static Jelly_Application instance;
    private final int MY_PERMISSIONS_REQUEST    = 100;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    private final String[] permissionMainName = { Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA};

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public static Jelly_Application getInstance() {
        return instance;
    }

}
