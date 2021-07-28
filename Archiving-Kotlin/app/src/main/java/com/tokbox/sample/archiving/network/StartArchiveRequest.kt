package com.tokbox.sample.archiving.network

import com.squareup.moshi.Json

class StartArchiveRequest {
    @Json(name = "sessionId")
    var sessionId: String? = null
}