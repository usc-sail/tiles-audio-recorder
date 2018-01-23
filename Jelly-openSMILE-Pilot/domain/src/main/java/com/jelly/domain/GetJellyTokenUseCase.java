package com.jelly.domain;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import jelly.data.entities.GetJellyTokenIDResponse;
import jelly.data.repositories.JellyTokenRespository;

/**
 * Created by tiantianfeng on 11/30/17.
 */

public class GetJellyTokenUseCase extends UseCase<GetJellyTokenIDResponse>{
    private JellyTokenRespository jellyRepo;
    private Scheduler mExecutorThread;
    private Scheduler mMainThread;
    private String jelly_token_id;
    private String state, result, message, errors;
    private int estimoteResponse;

    public GetJellyTokenUseCase(JellyTokenRespository jellyTokenRespository, Scheduler executorThread, Scheduler mainThread) {
        jellyRepo = jellyTokenRespository;
        mExecutorThread = executorThread;
        mMainThread = mainThread;
    }

    public void passJellyTokenParams(String jelly_token_id) {
        this.jelly_token_id = jelly_token_id;
    }

    @Override
    public Observable<GetJellyTokenIDResponse> buildObservable() {
        return jellyRepo.getJellyTokenIDResponse(jelly_token_id).subscribeOn(mExecutorThread).observeOn(mMainThread);
    }
}
