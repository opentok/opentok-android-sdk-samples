package com.tokbox.sample.basicvideochatconnectionservice

import android.text.TextUtils

object OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects

    Note that this application will ignore credentials in the `OpenTokConfig` file when `CHAT_SERVER_URL` contains a
    valid URL.
    */
    // Replace with a API key
    const val API_KEY: String = ""

    // Replace with a generated Session ID
    const val SESSION_ID: String = ""

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    const val TOKEN: String = ""

    val isValid: Boolean
        // *** The code below is to validate this configuration file. You do not need to modify it  ***
        get() {
            if (TextUtils.isEmpty(API_KEY)
                || TextUtils.isEmpty(SESSION_ID)
                || TextUtils.isEmpty(TOKEN)
            ) {
                return false
            }

            return true
        }

    val description: String
        get() = ("""
     OpenTokConfig:
     API_KEY: $API_KEY
     SESSION_ID: $SESSION_ID
     TOKEN: $TOKEN
     
     """.trimIndent())
}
