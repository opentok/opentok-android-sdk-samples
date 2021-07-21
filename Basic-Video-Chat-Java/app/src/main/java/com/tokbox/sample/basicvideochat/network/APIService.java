package com.tokbox.sample.basicvideochat.network;

import retrofit2.Call;
import retrofit2.http.*;

public interface APIService {

    @GET("session")
    Call<GetSessionResponse> getSession();
}
