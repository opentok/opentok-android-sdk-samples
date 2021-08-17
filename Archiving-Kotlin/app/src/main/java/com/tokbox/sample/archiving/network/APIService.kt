package com.tokbox.sample.archiving.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface APIService {

    @get:GET("session")
    val session: Call<GetSessionResponse?>?

    @POST("archive/start")
    @Headers("Content-Type: application/json")
    fun startArchive(@Body startArchiveRequest: StartArchiveRequest?): Call<Any?>

    @POST("archive/{archiveId}/stop")
    fun stopArchive(@Path("archiveId") archiveId: String?): Call<Any?>
}