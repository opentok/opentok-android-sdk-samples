package com.tokbox.sample.ee2evideo.network

import retrofit2.Call
import retrofit2.http.GET

interface APIService {

    @get:GET("session")
    val session: Call<GetSessionResponse?>?
}