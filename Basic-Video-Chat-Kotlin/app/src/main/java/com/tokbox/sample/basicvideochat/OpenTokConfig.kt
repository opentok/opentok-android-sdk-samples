package com.tokbox.sample.basicvideochat

import android.text.TextUtils

object OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects

    Note that this application will ignore credentials in the `OpenTokConfig` file when `CHAT_SERVER_URL` contains a
    valid URL.
    */
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects

    Note that this application will ignore credentials in the `OpenTokConfig` file when `CHAT_SERVER_URL` contains a
    valid URL.
    */

    // Replace with a API key
    const val API_KEY = ""

    // Replace with a generated Session ID
    const val SESSION_ID = ""

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    const val TOKEN = ""

    // *** The code below is to validate this configuration file. You do not need to modify it  ***
    val isValid: Boolean
        get() = !(TextUtils.isEmpty(API_KEY) || TextUtils.isEmpty(SESSION_ID) || TextUtils.isEmpty(TOKEN))

    val description: String
        get() = """
               OpenTokConfig:
               API_KEY: $API_KEY
               SESSION_ID: $SESSION_ID
               TOKEN: $TOKEN
               """.trimIndent()
}