package com.tokbox.sample.basicvideochat_connectionservice.network;

import retrofit2.Call;
import retrofit2.http.*;

public interface APIService {

    @GET("session")
    Call<GetSessionResponse> getSession();
}
