package com.tokbox.sample.videotransformers.network

import com.squareup.moshi.Json

class GetSessionResponse {

    @Json(name = "apiKey")
    var apiKey: String = ""

    @Json(name = "sessionId")
    var sessionId: String = ""

    @Json(name = "token")
    var token: String = ""
}