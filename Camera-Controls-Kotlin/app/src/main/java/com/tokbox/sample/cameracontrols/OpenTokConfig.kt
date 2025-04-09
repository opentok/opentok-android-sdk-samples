package com.tokbox.sample.cameracontrols

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
    const val API_KEY = "47521351"

    // Replace with a generated Session ID
    const val SESSION_ID = "1_MX40NzUyMTM1MX5-MTcxODM1OTEyMzUyNH5oaUhWbUx4TXJQdldMY2tmRGJ1bFB3a2Z-fn4"

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    const val TOKEN = "T1==cGFydG5lcl9pZD00NzUyMTM1MSZzaWc9OWU0YTYyZWVlMDk1YjY4YmFlNjhmZDRjZjkyYjU3ODU4MjcxZjQyNzpzZXNzaW9uX2lkPTFfTVg0ME56VXlNVE0xTVg1LU1UY3hPRE0xT1RFeU16VXlOSDVvYVVoV2JVeDRUWEpRZGxkTVkydG1SR0oxYkZCM2EyWi1mbjQmY3JlYXRlX3RpbWU9MTcxODM1OTEyOCZub25jZT0wLjAxMTc0MTEzNTQ5NDAxOTExNyZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNzIwOTUxMTI3JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9"

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