package com.tokbox.sample.archiving;

import android.webkit.URLUtil;
import androidx.annotation.NonNull;

public class ServerConfig {

    // ********  Enter the server URLs containing session and archiving resources below   ********
    //     For example (if using a heroku subdomain): "https://yoursubdomain.herokuapp.com"
    //
    //     If you are running on localhost, you will need to take the following steps:
    //          1. Make sure your device is on the same network as the computer running your server
    //          2. Determine your computer's local network IP address
    //          3. Enter "http://<your computer's local IP>:<port number>"
    //              for example: "http://192.168.1.103:8080"
    //
    //
    // Note that hard coding session information will not work if you are using archiving
    // To quickly set up a pre-made web service, see https://github.com/opentok/learning-opentok-php
    //

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
