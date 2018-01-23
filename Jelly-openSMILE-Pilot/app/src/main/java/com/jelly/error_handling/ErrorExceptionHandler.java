package com.jelly.error_handling;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import com.jelly.application.Jelly_Application;
import com.jelly.opensmile.OpenSmile_VAD;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by tiantianfeng on 11/3/17.
 */

public class ErrorExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Activity activity;
    private final Context myContext;
    private final Class<?> myActivityClass;

    public ErrorExceptionHandler(Context context, Class<?> c) {
        myContext = context;
        myActivityClass = c;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        System.err.println(stackTrace);// You can use LogCat too
        saveErrorToCSV(stackTrace.toString());
        Intent intent = new Intent(myContext, myActivityClass);
        String s = stackTrace.toString();
        //you can use this String to know what caused the exception and in which Activity
        intent.putExtra("uncaughtException", "Exception is: " + stackTrace.toString());
        intent.putExtra("stacktrace", s);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.
                getActivity(Jelly_Application.getInstance()
                        .getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

        AlarmManager mgr = (AlarmManager) Jelly_Application.getInstance()
                .getBaseContext().getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);

        activity.finish();
        Process.killProcess(Process.myPid());

        //System.exit(2);
    }

    private void saveErrorToCSV(String error) {

        String datapath = "TILEs";
        File file = new File(Environment.getExternalStorageDirectory(), datapath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("TILEs", "Problem creating folder");
            }
        }

        String filepath = Environment.getExternalStorageDirectory().getPath() + "/TILEs/Error_Log.csv";

        Calendar calendar = Calendar.getInstance();
        String vadTimestamp = Long.toString(new Date().getTime());


        try (CSVWriter writer = new CSVWriter(new FileWriter(filepath, true))) {

            String[] data = new String[2];

            data[0] = vadTimestamp;
            data[1] = error;

            writer.writeNext(data);
            writer.close();

        } catch (IOException e) {

        }

    }
}
