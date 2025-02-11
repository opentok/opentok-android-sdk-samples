package com.tokbox.sample.e2eevideochat.network

import retrofit2.Call
import retrofit2.http.GET

interface APIService {

    @get:GET("session")
    val session: Call<GetSessionResponse?>?
}