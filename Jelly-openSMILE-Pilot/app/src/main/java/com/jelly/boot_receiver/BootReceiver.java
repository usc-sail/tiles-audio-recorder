package com.jelly.boot_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jelly.battery.Battery_Service;
import com.jelly.constant.Constants;
import com.jelly.main.Main;
import com.jelly.opensmile.OpenSmile_Service;
import com.jelly.opensmile.OpenSmile_VAD;

import org.radarcns.android.MainActivity;

import java.io.File;

/**
 * Created by tiantianfeng on 10/25/17.
 */

public class BootReceiver extends BroadcastReceiver {

    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {

            /*
            * VAD_STATUS: so VAD will start by Battery Service
            * VAD_WRITE: so VAD GAP and VAD TIME will be write a new value
            * */
            writeSharedPreference(Constants.VAD_STATUS, Constants.VAD_DEAD);
            writeSharedPreference(Constants.VAD_WRITE,  Constants.FALSE);
            writeSharedPreference(Constants.BLE_STATUS, Constants.BLE_OFF);
            writeSharedPreference(Constants.UPLOAD_STATUS, Constants.UPLOAD_OFF);

            Intent MainActivity_Intent = new Intent(context, Main.class);

            MainActivity_Intent.setAction(Intent.ACTION_MAIN);
            MainActivity_Intent.addCategory(Intent.CATEGORY_LAUNCHER);
            MainActivity_Intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(MainActivity_Intent);
        }
    }

    private void writeSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }



}
