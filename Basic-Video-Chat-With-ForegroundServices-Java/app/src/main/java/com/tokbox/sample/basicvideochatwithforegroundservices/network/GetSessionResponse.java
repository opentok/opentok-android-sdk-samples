package com.tokbox.sample.basicvideochatwithforegroundservices.network;

import com.squareup.moshi.Json;

public class GetSessionResponse {
    @Json(name = "apiKey") public String apiKey;
    @Json(name = "sessionId") public String sessionId;
    @Json(name = "token") public String token;
}
