package com.tokbox.sample.archiving.network;

import retrofit2.Call;
import retrofit2.http.*;

public interface APIService {

    @GET("session")
    Call<GetSessionResponse> getSession();

    @POST("archive/start")
    @Headers("Content-Type: application/json")
    Call<Void> startArchive(@Body StartArchiveRequest startArchiveRequest);

    @GET("archive/{archiveId}/stop")
    Call<Void> stopArchive(@Path("archiveId") String archiveId);
}
