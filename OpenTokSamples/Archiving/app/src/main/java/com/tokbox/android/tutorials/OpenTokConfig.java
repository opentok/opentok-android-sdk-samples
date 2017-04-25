package com.tokbox.android.tutorials;

import android.webkit.URLUtil;

public class OpenTokConfig {

    // ********  Enter the server URLs containing session and archiving resources below   ********
    //     For example (if using a heroku subdomain): "https://yoursubdomain.herokuapp.com"
    //
    // Note that hard coding session information will not work if you are using archiving
    // To quickly set up a pre-made web service, see https://github.com/opentok/learning-opentok-php
    //

    public static final String CHAT_SERVER_URL = "https://opentokphpservice.herokuapp.com";
    public static final String SESSION_INFO_ENDPOINT = CHAT_SERVER_URL + "/session";
    public static final String ARCHIVE_START_ENDPOINT = CHAT_SERVER_URL + "/start/:sessionId";
    public static final String ARCHIVE_STOP_ENDPOINT = CHAT_SERVER_URL + "/stop/:archiveId";
    public static final String ARCHIVE_PLAY_ENDPOINT = CHAT_SERVER_URL + "/view/:archiveId";

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
