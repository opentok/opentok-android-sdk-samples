package com.tokbox.sample.basicvideochat

import android.text.TextUtils
import android.webkit.URLUtil

object ServerConfig {
    /*
    You can set up a server to provide session information. To quickly set up a pre-made web service, see
    https://github.com/opentok/learning-opentok-php
    or
    https://github.com/opentok/learning-opentok-node

    After deploying the server open the `ServerConfig` file in this project and configure the `CHAT_SERVER_URL`
    with your domain to fetch credentials from the server.

    Note that this application will ignore credentials in the `OpenTokConfig` file when `CHAT_SERVER_URL` contains a
    valid URL.
    */
    const val CHAT_SERVER_URL: String = ""

    // *** The code below is to validate this configuration file. You do not need to modify it  ***
    fun hasChatServerUrl() = !TextUtils.isEmpty(CHAT_SERVER_URL)

    val isValid: Boolean
        get() {
            if (!URLUtil.isHttpsUrl(CHAT_SERVER_URL) && !URLUtil.isHttpUrl(CHAT_SERVER_URL)) {
                return false
            } else if (!URLUtil.isValidUrl(CHAT_SERVER_URL)) {
                return false
            }
            return true
        }
    val description: String
        get() = "ServerConfig. CHAT_SERVER_URL: $CHAT_SERVER_URL"
}