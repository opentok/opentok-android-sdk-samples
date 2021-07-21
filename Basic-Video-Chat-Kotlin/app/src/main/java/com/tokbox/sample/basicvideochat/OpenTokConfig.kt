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
    const val API_KEY = "47215304"

    // Replace with a generated Session ID
    const val SESSION_ID = "1_MX40NzIxNTMwNH5-MTYyNjEwNDgxOTY1Mn5kb3VXa0hnaHV0TGRaL29iTjhNUGVqYmF-UH4"

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    const val TOKEN = "T1==cGFydG5lcl9pZD00NzIxNTMwNCZzaWc9ZTRjNjBmYzIwMjZhNmU0ZWE0ZWMxZmE2ZWYwY2I1YmRkMjY3MWIxNjpzZXNzaW9uX2lkPTFfTVg0ME56SXhOVE13Tkg1LU1UWXlOakV3TkRneE9UWTFNbjVrYjNWWGEwaG5hSFYwVEdSYUwyOWlUamhOVUdWcVltRi1VSDQmY3JlYXRlX3RpbWU9MTYyNjEwNDgzMiZub25jZT0wLjM4MDY4MDMyOTgzODE5MTEmcm9sZT1wdWJsaXNoZXImZXhwaXJlX3RpbWU9MTYyNjE5MTIzMSZpbml0aWFsX2xheW91dF9jbGFzc19saXN0PQ=="

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