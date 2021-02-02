package com.tokbox.sample.basicvideochat;

import android.text.TextUtils;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;

public class ServerConfig {

    /*                           ***** OPTIONAL *****
     If you have set up a server to provide session information replace the null value
     in CHAT_SERVER_URL with it.

     For example: "https://yoursubdomain.com"

     To quickly set up server, see https://github.com/opentok/learning-opentok-php
    */
    public static final String CHAT_SERVER_URL = "";

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
