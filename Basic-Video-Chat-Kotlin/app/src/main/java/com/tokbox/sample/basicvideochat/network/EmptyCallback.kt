package com.tokbox.sample.basicvideochat.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmptyCallback<T> : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {}
    override fun onFailure(call: Call<T>, t: Throwable) {}
}