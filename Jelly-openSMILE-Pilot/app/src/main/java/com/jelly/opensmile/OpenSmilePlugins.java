package com.jelly.opensmile;

import android.app.Activity;
import android.app.Service;
import android.os.Environment;
import android.util.Log;

import com.audeering.opensmile.androidtemplate.SmileJNI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by tiantianfeng on 10/18/17.
 */

public class OpenSmilePlugins implements SmileJNI.Listener {

    public static int i = 1;
    public static String features;
    public static String fileName;
    public static OutputStreamWriter outputStreamWriter;
    public File file;
    public String fpath1;
    File dir;
    private static Service act;

    public OpenSmilePlugins(String name) throws IOException {
        this.act = act;
        SmileJNI.registerListener(this);
    }

    @Override
    public void onSmileMessageReceived(String text) {
        final String t = text;

        try {
            JSONObject jo = new JSONObject(t);

            String messageType = jo.getString("msgname");

            double data1 = jo.getJSONObject("floatData").getDouble("0");

            if(data1 == 1) {
                Log.d("Tiles:", "VAD FOUND" + ": " + data1);

            }


            /*
            double treb = jo.getJSONObject("floatData").getDouble("0");
            double mid = jo.getJSONObject("floatData").getDouble("1");
            double bas = jo.getJSONObject("floatData").getDouble("2");
            features = treb + "," + mid + "," + bas + "\n";
            //writeToFile(features);

            String messageType = jo.getString("msgname");

            double data1 = jo.getJSONObject("floatData").getDouble("0");
            double data2 = jo.getJSONObject("floatData").getDouble("1");
            double data3 = jo.getJSONObject("floatData").getDouble("2");
            double data4 = jo.getJSONObject("floatData").getDouble("3");
            double data5 = jo.getJSONObject("floatData").getDouble("4");
            double data6 = jo.getJSONObject("floatData").getDouble("5");
            double data7 = jo.getJSONObject("floatData").getDouble("6");
            double data8 = jo.getJSONObject("floatData").getDouble("7");

            Log.d("Tiles:", messageType + ": " + data1 + "," + data2 + ","
                    + data3 + "," + data4 + "," + data5 + "," + data6 + ","
                    + data7 + "," + data8);*/

            //Log.d("Tiles:", jo.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void writeToFile(String data) {
        try {
            outputStreamWriter.append(data);
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void closeFile() throws IOException {
        outputStreamWriter.close();
    }
    public String getFilePath() throws IOException {
        return fpath1;
    }


}
