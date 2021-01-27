package com.tokbox.sample.archiving;

import android.webkit.URLUtil;

public class OpenTokConfig {

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

    public static void verifyChatServerUrl(){
        if (OpenTokConfig.CHAT_SERVER_URL == null) {
            throw new RuntimeException("CHAT_SERVER_URL in OpenTokConfig.java must not be null");
        } else if ( !( URLUtil.isHttpsUrl(OpenTokConfig.CHAT_SERVER_URL) || URLUtil.isHttpUrl(OpenTokConfig.CHAT_SERVER_URL)) ) {
            throw new RuntimeException("CHAT_SERVER_URL in OpenTokConfig.java must be specified as either  http or " +
                    "https");
        } else if ( !URLUtil.isValidUrl(OpenTokConfig.CHAT_SERVER_URL) ) {
            throw new RuntimeException("CHAT_SERVER_URL in OpenTokConfig.java is not a valid URL");
        }
    }
}
