package jelly.data.rest;

import io.reactivex.Observable;
import jelly.data.entities.GetJellyTokenIDResponse;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by niranjanb on 05/11/17.
 */

interface EndPoints {

    @GET("link_jelly/{JellyToken}")
    Observable<GetJellyTokenIDResponse> getJellyTokenIDResponse(@Path("JellyToken") String jellyTokenID);

}
