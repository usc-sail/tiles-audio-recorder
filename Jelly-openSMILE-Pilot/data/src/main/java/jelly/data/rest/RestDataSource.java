package jelly.data.rest;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import io.reactivex.Observable;
import jelly.data.entities.GetJellyTokenIDResponse;
import jelly.data.repositories.JellyTokenRespository;
import jelly.data.utils.URLMapper;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Tiantian on 05/11/17.
 */

public class RestDataSource implements JellyTokenRespository {

    private EndPoints mEndPoints;

    public RestDataSource() {
        Retrofit retrofit = new Retrofit.Builder()
                                    .baseUrl(URLMapper.BASE_URL)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                                    .build();
        mEndPoints = retrofit.create(EndPoints.class);
    }
    @Override
    public Observable<GetJellyTokenIDResponse> getJellyTokenIDResponse(String jellyTokenID) {
        return mEndPoints.getJellyTokenIDResponse(jellyTokenID);
    }

}
