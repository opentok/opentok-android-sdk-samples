package com.tokbox.sample.archiving;

import android.webkit.URLUtil;
import androidx.annotation.NonNull;

public class ServerConfig {

    /*
    To quickly set up a pre-made web service, see
    https://github.com/opentok/learning-opentok-php
    or
    https://github.com/opentok/learning-opentok-node

    After deploying the server open the `ServerConfig` file in this project and configure the `CHAT_SERVER_URL`
    with your domain to fetch credentials from the server.
    */

    public static final String CHAT_SERVER_URL = "";

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
