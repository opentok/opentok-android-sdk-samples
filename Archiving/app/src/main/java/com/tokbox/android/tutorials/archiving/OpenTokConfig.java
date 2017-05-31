package com.tokbox.android.tutorials.archiving;

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


    public static final String CHAT_SERVER_URL = null;
    public static final String SESSION_INFO_ENDPOINT = CHAT_SERVER_URL + "/session";
    public static final String ARCHIVE_START_ENDPOINT = CHAT_SERVER_URL + "/archive/start";
    public static final String ARCHIVE_STOP_ENDPOINT = CHAT_SERVER_URL + "/archive/:archiveId/stop";
    public static final String ARCHIVE_PLAY_ENDPOINT = CHAT_SERVER_URL + "/archive/:archiveId/view";

    public static String configErrorMessage;

    public static boolean isConfigUrlValid(){
        if (OpenTokConfig.CHAT_SERVER_URL == null) {
            configErrorMessage = "CHAT_SERVER_URL in OpenTokConfig.java must not be null";
            return false;
        } else if ( !( URLUtil.isHttpsUrl(OpenTokConfig.CHAT_SERVER_URL) || URLUtil.isHttpUrl(OpenTokConfig.CHAT_SERVER_URL)) ) {
            configErrorMessage = "CHAT_SERVER_URL in OpenTokConfig.java must be specified as either  http or https";
            return false;
        } else if ( !URLUtil.isValidUrl(OpenTokConfig.CHAT_SERVER_URL) ) {
            configErrorMessage = "CHAT_SERVER_URL in OpenTokConfig.java is not a valid URL";
            return false;
        } else {
            return true;
        }
    }
}
