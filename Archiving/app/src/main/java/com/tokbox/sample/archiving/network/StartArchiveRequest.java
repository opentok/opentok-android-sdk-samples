package com.tokbox.sample.archiving.network;

import com.squareup.moshi.Json;

public class StartArchiveRequest {
    @Json(name = "sessionId") public String sessionId;
}
