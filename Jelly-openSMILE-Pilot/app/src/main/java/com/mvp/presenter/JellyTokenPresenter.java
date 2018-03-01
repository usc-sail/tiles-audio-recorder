package com.mvp.presenter;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jelly.constant.Constants;
import com.jelly.domain.GetJellyTokenUseCase;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jelly.data.entities.GetJellyTokenIDResponse;
import jelly.data.rest.RestDataSource;

/**
 * Created by tiantianfeng on 11/30/17.
 */

public class JellyTokenPresenter extends Service{

    private GetJellyTokenUseCase mJellyTokenUseCase;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mJellyTokenUseCase = new GetJellyTokenUseCase(new RestDataSource(),
                Schedulers.newThread(),
                AndroidSchedulers.mainThread());

        Log.d("TILES", "JellyTokenService -> onCreate -> " + retrieveSharedPreference(Constants.QR_CODE_ID));
        requestJellyToken(retrieveSharedPreference(Constants.QR_CODE_ID));
    }

    public void requestJellyToken(String jellyToken) {

        mJellyTokenUseCase.passJellyTokenParams(jellyToken);

        mJellyTokenUseCase.execute().subscribe(new Observer<GetJellyTokenIDResponse>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d("TILEs", "JellyTokenPresenter->GetJellyTokenIDResponse->onSubscribe");
            }

            @Override
            public void onNext(GetJellyTokenIDResponse value) {
                Log.d("TILEs", "JellyTokenPresenter->GetJellyTokenIDResponse->onNext:" + value.toString());
                if(value.toString().contains("true")){
                    writeSharedPreference(Constants.QR_CODE_SYNC, Constants.QR_CODE_SYNCED);
                    Log.d("TILEs", "JellyTokenPresenter->GetJellyTokenIDResponse->onNext: SYNCED");
                }
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                Log.d("TILEs", "JellyTokenPresenter->GetJellyTokenIDResponse->onComplete");
                stopSelf();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
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
