package com.tokbox.sample.basicvoipcall;

import android.text.TextUtils;
import androidx.annotation.NonNull;

public class OpenTokConfig {
    /*
    Fill the following variables using your own Project info from the OpenTok dashboard
    https://dashboard.tokbox.com/projects
    */

    // Replace with a API key
    public static final String API_KEY = "100";

    // Replace with a generated Session ID
    public static final String SESSION_ID = "2_MX4xMDB-fjE2NDg2NDg0MDg0NjB-eURucXdtNXlhVXJ0eUJuYkdTSGRaU2JUfn4";

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "T1==cGFydG5lcl9pZD0xMDAmc2lnPTBlYzUzY2I4MzIxOTc4MmFlM2I3NmZiYmY4ZjQ0ZjU3MGRlYzg2NDY6c2Vzc2lvbl9pZD0yX01YNHhNREItZmpFMk5EZzJORGcwTURnME5qQi1lVVJ1Y1hkdE5YbGhWWEowZVVKdVlrZFRTR1JhVTJKVWZuNCZjcmVhdGVfdGltZT0xNjQ4NjQ4NDA4Jm5vbmNlPTAuNzIxMTIxMTM1MzQ2OTcmcm9sZT1tb2RlcmF0b3ImZXhwaXJlX3RpbWU9MTY0ODczNDgwOCZpbml0aWFsX2xheW91dF9jbGFzc19saXN0PQ==";

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
