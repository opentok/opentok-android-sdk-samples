package com.tokbox.sample.basicvideorenderer;

import android.text.TextUtils;
import androidx.annotation.NonNull;

public class OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects
    */

    // Replace with a API key
    public static final String API_KEY = "46433782";
    
    // Replace with a generated Session ID
    public static final String SESSION_ID = "1_MX40NjQzMzc4Mn5-MTY0NDQ3ODA0NzcyOX5lZnhjL0xLak90SU9xa1JvL21BZWFwV0F-fg";
    
    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD00NjQzMzc4MiZzaWc9Mjk4MGIwMDcwMjVhMzU5MWVjMGI5NzU2MTgzNDc4ZGU0MmQ3YWIyOTpzZXNzaW9uX2lkPTFfTVg0ME5qUXpNemM0TW41LU1UWTBORFEzT0RBME56Y3lPWDVsWm5oakwweExhazkwU1U5eGExSnZMMjFCWldGd1YwRi1mZyZjcmVhdGVfdGltZT0xNjU5MzYzNTA2Jm5vbmNlPTAuOTc4Mzg5NTkyOTg5MjI1NSZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNjU5NDQ5OTA2JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";

    public static boolean isValid() {
        if (TextUtils.isEmpty(OpenTokConfig.API_KEY)
                || TextUtils.isEmpty(OpenTokConfig.SESSION_ID)
                || TextUtils.isEmpty(OpenTokConfig.TOKEN)) {
            return false;
        }

        return true;
    }

    @NonNull
    public static String getDescription() {
        return "OpenTokConfig:" + "\n"
                + "API_KEY: " + OpenTokConfig.API_KEY + "\n"
                + "SESSION_ID: " + OpenTokConfig.SESSION_ID + "\n"
                + "TOKEN: " + OpenTokConfig.TOKEN + "\n";
    }
}

