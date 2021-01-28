package com.tokbox.sample.framemetadata;

import android.text.TextUtils;

public class OpenTokConfig {
    // *** Fill the following variables using your own Project info from the OpenTok dashboard  ***
    // ***                      https://dashboard.tokbox.com/projects                           ***

    // Replace with your OpenTok API key
    public static final String API_KEY = "";
    // Replace with a generated Session ID
    public static final String SESSION_ID = "";
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "";

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
}
