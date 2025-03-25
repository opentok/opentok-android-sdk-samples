package com.tokbox.sample.enabletorch;

import android.text.TextUtils;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;

public class ServerConfig {
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
    public static final String CHAT_SERVER_URL = "https://YOURAPPNAME.herokuapp.com";

    // *** The code below is to validate this configuration file. You do not need to modify it  ***

    public static boolean hasChatServerUrl() {
        return !TextUtils.isEmpty(CHAT_SERVER_URL);
    }

    public static boolean isValid() {
        if (ServerConfig.CHAT_SERVER_URL == null) {
            return false;
        } else if (!(URLUtil.isHttpsUrl(ServerConfig.CHAT_SERVER_URL) || URLUtil.isHttpUrl(ServerConfig.CHAT_SERVER_URL))) {
            return false;
        } else if (!URLUtil.isValidUrl(ServerConfig.CHAT_SERVER_URL)) {
            return false;
        }

        return true;
    }

    @NonNull
    public static String getDescription() {
        return "ServerConfig. CHAT_SERVER_URL: " + CHAT_SERVER_URL;
    }
}
