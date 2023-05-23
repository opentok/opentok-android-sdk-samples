package com.tokbox.sample.basicvideochat.network;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmptyCallback<T> implements Callback<T> {
    @Override
    public void onResponse(Call<T> call, Response<T> response) {

    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {

    }
}
