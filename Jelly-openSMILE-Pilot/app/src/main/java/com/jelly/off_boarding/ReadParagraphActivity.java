package com.jelly.off_boarding;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jelly.constant.Constants;
import com.jelly.main.Main;

import java.util.ArrayList;

/**
 * Created by tiantianfeng on 1/10/18.
 */

public class ReadParagraphActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(Constants.OPENSMILE_CURRENT_ON.contains(Constants.VAD_ON) || Constants.VAD_STATUS.contains(Constants.VAD_ON)) {

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
