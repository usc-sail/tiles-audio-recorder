package jelly.data.repositories;

import io.reactivex.Observable;
import jelly.data.entities.GetJellyTokenIDResponse;

/**
 * Created by tiantianfeng on 11/30/17.
 */

public interface JellyTokenRespository {
    Observable<GetJellyTokenIDResponse> getJellyTokenIDResponse(String jellyTokenID);
}
