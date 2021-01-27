package com.tokbox.sample.basicvideochat;

import android.text.TextUtils;
import android.webkit.URLUtil;

public class OpenTokConfig {
    // *** Fill the following variables using your own Project info from the OpenTok dashboard  ***
    // ***                      https://dashboard.tokbox.com/projects                           ***

    // Replace with your OpenTok API key
    public static final String API_KEY = "";
    // Replace with a generated Session ID
    public static final String SESSION_ID = "";
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "";

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

    public static void verifyConfig() {
        if (TextUtils.isEmpty(OpenTokConfig.API_KEY)) {
            throw new RuntimeException("API_KEY in OpenTokConfig.java cannot be null or empty");
        }

        if (TextUtils.isEmpty(OpenTokConfig.SESSION_ID)) {
            throw new RuntimeException("SESSION_ID in OpenTokConfig.java cannot be null or empty");
        }

        if (TextUtils.isEmpty(OpenTokConfig.TOKEN)) {
            throw new RuntimeException("TOKEN in OpenTokConfig.java cannot be null or empty");
        }
    }

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
